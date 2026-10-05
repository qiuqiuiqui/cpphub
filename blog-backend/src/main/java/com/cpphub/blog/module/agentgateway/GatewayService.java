package com.cpphub.blog.module.agentgateway;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cpphub.blog.common.Result;
import com.cpphub.blog.entity.User;
import com.cpphub.blog.mapper.UserMapper;
import com.cpphub.blog.module.agentgateway.dto.AgentEnvelope;
import com.cpphub.blog.module.agentgateway.dto.DeviceStatus;
import com.cpphub.blog.module.agentgateway.entity.AgentCommandLog;
import com.cpphub.blog.module.agentgateway.entity.AgentDevice;
import com.cpphub.blog.module.agentgateway.entity.AgentPairingCode;
import com.cpphub.blog.module.agentgateway.mapper.AgentCommandLogMapper;
import com.cpphub.blog.module.agentgateway.mapper.AgentDeviceMapper;
import com.cpphub.blog.module.agentgateway.mapper.AgentPairingCodeMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Central gateway that brokers:
 *   - pairing codes (HTTP)
 *   - WSS sessions (per device)
 *   - command dispatch (REST → WSS → response)
 *
 * Threading: WebSocketSession is NOT thread-safe, so we use synchronized(session) when writing.
 * Pending commands are stored in ConcurrentHashMap keyed by request id, completed by the WSS
 * inbound handler. Callers block on a CompletableFuture with a hard timeout.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayService {
  private final AgentDeviceMapper deviceMapper;
  private final AgentPairingCodeMapper pairingMapper;
  private final AgentCommandLogMapper commandLogMapper;
  private final UserMapper userMapper;
  private final ObjectMapper objectMapper;
  private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
  /** requestId -> pending future for the caller */
  private final Map<String, CompletableFuture<AgentEnvelope>> pending = new ConcurrentHashMap<>();
  /** executor for timeouts */
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  @PreDestroy
  public void shutdown() {
    scheduler.shutdownNow();
    sessions.values().forEach(s -> {
      try {
        s.close();
      } catch (Exception ignore) {
      }
    });
  }

  // ---------------- Pairing (HTTP) ----------------

  /** Generate a fresh 6-digit pairing code for the given admin user. */
  @Transactional
  public String createPairingCode(Long userId, String deviceName) {
    // invalidate any unused codes for this user
    pairingMapper.delete(Wrappers.<AgentPairingCode>lambdaQuery()
            .eq(AgentPairingCode::getUserId, userId)
            .isNull(AgentPairingCode::getUsedAt));

    AgentPairingCode pc = new AgentPairingCode();
    pc.setCode(String.format("%06d", RandomUtil.randomInt(0, 1_000_000)));
    pc.setUserId(userId);
    pc.setDeviceName(deviceName);
    pc.setExpiresAt(LocalDateTime.now().plus(Duration.ofMinutes(10)));
    pairingMapper.insert(pc);
    return pc.getCode();
  }

  /**
   * Redeem a pairing code. Returns (deviceId, jwt) on success.
   * The JWT is a small stateless token (HS256 with a static secret) for MVP simplicity.
   */
  @Transactional
  public Result<PairResult> redeemCode(String code, String os, String arch, String agentVersion) {
    AgentPairingCode pc = pairingMapper.selectOne(
        Wrappers.<AgentPairingCode>lambdaQuery().eq(AgentPairingCode::getCode, code));
    if (pc == null) {
      return Result.fail(4001, "invalid code");
    }
    if (pc.getUsedAt() != null) {
      return Result.fail(4002, "code already used");
    }
    if (pc.getExpiresAt().isBefore(LocalDateTime.now())) {
      return Result.fail(4003, "code expired");
    }

    // Mark used
    pc.setUsedAt(LocalDateTime.now());
    pairingMapper.updateById(pc);

    // Create or refresh device
    String deviceId = IdUtil.fastSimpleUUID();
    String rawJwt = JwtUtil.mint(deviceId, pc.getUserId(), Duration.ofDays(30));
    AgentDevice dev = new AgentDevice();
    dev.setDeviceId(deviceId);
    dev.setUserId(pc.getUserId());
    dev.setName(
        pc.getDeviceName() != null ? pc.getDeviceName() : ("Device-" + deviceId.substring(0, 8)));
    dev.setOs(os);
    dev.setArch(arch);
    dev.setAgentVersion(agentVersion);
    dev.setJwtHash(sha256(rawJwt));
    dev.setStatus("OFFLINE");
    deviceMapper.insert(dev);

    return Result.ok(new PairResult(deviceId, rawJwt));
  }

  public record PairResult(String deviceId, String jwt) {}

  /** Resolve the User.id for a given username (used by REST controllers for auth). */
  public Long resolveUserIdByUsername(String username) {
    if (username == null)
      return null;
    User u = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
    return u == null ? null : u.getId();
  }
  // ---------------- WSS lifecycle ----------------

  public void onAgentConnected(WebSocketSession session, String deviceId) {
    sessions.put(deviceId, session);
    AgentDevice dev = deviceMapper.selectOne(
        Wrappers.<AgentDevice>lambdaQuery().eq(AgentDevice::getDeviceId, deviceId));
    if (dev != null) {
      dev.setStatus("ONLINE");
      dev.setLastSeenAt(LocalDateTime.now());
      deviceMapper.updateById(dev);
    }
    log.info("agent online: deviceId={}", deviceId);
  }

  public void onAgentDisconnected(String deviceId) {
    sessions.remove(deviceId);
    AgentDevice dev = deviceMapper.selectOne(
        Wrappers.<AgentDevice>lambdaQuery().eq(AgentDevice::getDeviceId, deviceId));
    if (dev != null) {
      dev.setStatus("OFFLINE");
      deviceMapper.updateById(dev);
    }
    // Fail any pending requests so callers don't hang.
    pending.forEach(
        (id, fut) -> fut.completeExceptionally(new RuntimeException("agent disconnected")));
    pending.clear();
    log.info("agent offline: deviceId={}", deviceId);
  }

  /** Called by the WSS inbound handler when a CMD_RESPONSE arrives. */
  public void deliverResponse(AgentEnvelope env) {
    CompletableFuture<AgentEnvelope> fut = pending.remove(env.getId());
    if (fut != null) {
      fut.complete(env);
    }
  }

  public void touchLastSeen(String deviceId) {
    AgentDevice dev = deviceMapper.selectOne(
        Wrappers.<AgentDevice>lambdaQuery().eq(AgentDevice::getDeviceId, deviceId));
    if (dev != null) {
      dev.setLastSeenAt(LocalDateTime.now());
      deviceMapper.updateById(dev);
    }
  }

  // ---------------- REST API ----------------

  public List<DeviceStatus> listDevices(Long userId) {
    return deviceMapper
        .selectList(Wrappers.<AgentDevice>lambdaQuery()
                .eq(AgentDevice::getUserId, userId)
                .orderByDesc(AgentDevice::getCreatedAt))
        .stream()
        .map(this::toStatus)
        .toList();
  }

  /** List all devices (no user filter). Used for anonymous access where appropriate. */
  public List<DeviceStatus> listDevicesAll() {
    return deviceMapper
        .selectList(Wrappers.<AgentDevice>lambdaQuery().orderByDesc(AgentDevice::getCreatedAt))
        .stream()
        .map(this::toStatus)
        .toList();
  }

  public DeviceStatus getDevice(Long userId, String deviceId) {
    AgentDevice dev = deviceMapper.selectOne(Wrappers.<AgentDevice>lambdaQuery()
            .eq(AgentDevice::getDeviceId, deviceId)
            .eq(AgentDevice::getUserId, userId));
    return dev == null ? null : toStatus(dev);
  }

  /** Get device by deviceId without checking owner userId. */
  public DeviceStatus getDeviceByDeviceId(String deviceId) {
    AgentDevice dev = deviceMapper.selectOne(
        Wrappers.<AgentDevice>lambdaQuery().eq(AgentDevice::getDeviceId, deviceId));
    return dev == null ? null : toStatus(dev);
  }

  /** Dispatch a command to the given device and synchronously wait for the response. */
  public Result<AgentEnvelope> runCommand(
      Long userId, String deviceId, String command, Map<String, Object> params, long timeoutMs) {
    AgentDevice dev = deviceMapper.selectOne(Wrappers.<AgentDevice>lambdaQuery()
            .eq(AgentDevice::getDeviceId, deviceId)
            .eq(AgentDevice::getUserId, userId));
    if (dev == null) {
      return Result.fail(404, "device not found");
    }

    WebSocketSession session = sessions.get(deviceId);
    if (session == null || !session.isOpen()) {
      return Result.fail(503, "device offline");
    }

    String requestId = IdUtil.fastSimpleUUID();
    AgentEnvelope req = new AgentEnvelope();
    req.setType("CMD_REQUEST");
    req.setId(requestId);
    req.setCommand(command);
    req.setParams(params);
    req.setTs(System.currentTimeMillis());

    CompletableFuture<AgentEnvelope> fut = new CompletableFuture<>();
    pending.put(requestId, fut);

    long start = System.currentTimeMillis();
    try {
      synchronized (session) {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(req)));
      }
    } catch (Exception e) {
      pending.remove(requestId);
      return Result.fail(500, "send failed: " + e.getMessage());
    }

    AgentEnvelope resp;
    try {
      resp = fut.get(timeoutMs, TimeUnit.MILLISECONDS);
    } catch (TimeoutException te) {
      pending.remove(requestId);
      logCommand(requestId, deviceId, userId, command, params, -1,
          System.currentTimeMillis() - start, "TIMEOUT", "client timeout");
      return Result.fail(504, "agent timeout");
    } catch (Exception e) {
      pending.remove(requestId);
      logCommand(requestId, deviceId, userId, command, params, -1,
          System.currentTimeMillis() - start, "FAIL", e.getMessage());
      return Result.fail(500, "agent error: " + e.getMessage());
    }

    long dur = System.currentTimeMillis() - start;
    logCommand(requestId, deviceId, userId, command, params, 0, dur,
        resp.getError() == null ? "OK" : "FAIL", resp.getError());

    // Wrap response payload in a friendly envelope so the frontend can use Result<data>
    if (resp.getError() != null) {
      return Result.fail(500, resp.getError());
    }
    return Result.ok(resp);
  }

  /** Run a command against a device without validating the caller's userId. */
  public Result<AgentEnvelope> runCommandWithoutUserCheck(
      String deviceId, String command, Map<String, Object> params, long timeoutMs) {
    AgentDevice dev = deviceMapper.selectOne(
        Wrappers.<AgentDevice>lambdaQuery().eq(AgentDevice::getDeviceId, deviceId));
    if (dev == null) {
      return Result.fail(404, "device not found");
    }

    WebSocketSession session = sessions.get(deviceId);
    if (session == null || !session.isOpen()) {
      return Result.fail(503, "device offline");
    }

    String requestId = IdUtil.fastSimpleUUID();
    AgentEnvelope req = new AgentEnvelope();
    req.setType("CMD_REQUEST");
    req.setId(requestId);
    req.setCommand(command);
    req.setParams(params);
    req.setTs(System.currentTimeMillis());

    CompletableFuture<AgentEnvelope> fut = new CompletableFuture<>();
    pending.put(requestId, fut);

    long start = System.currentTimeMillis();
    try {
      synchronized (session) {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(req)));
      }
    } catch (Exception e) {
      pending.remove(requestId);
      return Result.fail(500, "send failed: " + e.getMessage());
    }

    AgentEnvelope resp;
    try {
      resp = fut.get(timeoutMs, TimeUnit.MILLISECONDS);
    } catch (TimeoutException te) {
      pending.remove(requestId);
      logCommand(requestId, deviceId, null, command, params, -1, System.currentTimeMillis() - start,
          "TIMEOUT", "client timeout");
      return Result.fail(504, "agent timeout");
    } catch (Exception e) {
      pending.remove(requestId);
      logCommand(requestId, deviceId, null, command, params, -1, System.currentTimeMillis() - start,
          "FAIL", e.getMessage());
      return Result.fail(500, "agent error: " + e.getMessage());
    }

    long dur = System.currentTimeMillis() - start;
    logCommand(requestId, deviceId, null, command, params, 0, dur,
        resp.getError() == null ? "OK" : "FAIL", resp.getError());

    if (resp.getError() != null) {
      return Result.fail(500, resp.getError());
    }
    return Result.ok(resp);
  }

  private void logCommand(String requestId, String deviceId, Long userId, String command,
      Map<String, Object> params, int exitCode, long dur, String status, String err) {
    try {
      AgentCommandLog log = new AgentCommandLog();
      log.setRequestId(requestId);
      log.setDeviceId(deviceId);
      log.setUserId(userId);
      log.setCommand(command);
      log.setParamsJson(params == null ? null : objectMapper.writeValueAsString(params));
      log.setExitCode(exitCode);
      log.setDurationMs(dur);
      log.setStatus(status);
      log.setErrorMsg(err);
      commandLogMapper.insert(log);
    } catch (Exception ex) {
      log.warn("failed to log command: {}", ex.getMessage());
    }
  }

  private DeviceStatus toStatus(AgentDevice dev) {
    return new DeviceStatus(dev.getDeviceId(), dev.getName(), dev.getOs(), dev.getAgentVersion(),
        dev.getStatus(), dev.getLastSeenAt(), dev.getCreatedAt());
  }

  private static String sha256(String s) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] hash = md.digest(s.getBytes());
      StringBuilder sb = new StringBuilder(hash.length * 2);
      for (byte b : hash) sb.append(String.format("%02x", b));
      return sb.toString();
    } catch (Exception e) {
      return "";
    }
  }
}