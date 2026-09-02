package com.cpphub.blog.module.agentgateway;

import com.cpphub.blog.common.Result;
import com.cpphub.blog.module.agentgateway.dto.AgentEnvelope;
import com.cpphub.blog.module.agentgateway.dto.DeviceStatus;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/admin/agent")
@RequiredArgsConstructor
public class AgentGatewayController {
  private final GatewayService gateway;

  @PostMapping("/pair-code")
  public Result<String> createPairCode(@RequestBody(required = false) Map<String, String> body) {
    Long userId = currentUserId();
    if (userId == null)
      return Result.fail(401, "not logged in");
    String deviceName = body == null ? null : body.get("deviceName");
    return Result.ok(gateway.createPairingCode(userId, deviceName));
  }

  @GetMapping("/devices")
  public Result<List<DeviceStatus>> listDevices() {
    Long userId = currentUserId();
    if (userId == null)
      return Result.fail(401, "not logged in");
    return Result.ok(gateway.listDevices(userId));
  }

  @GetMapping("/devices/{deviceId}")
  public Result<DeviceStatus> getDevice(@PathVariable String deviceId) {
    Long userId = currentUserId();
    if (userId == null)
      return Result.fail(401, "not logged in");
    DeviceStatus s = gateway.getDevice(userId, deviceId);
    return s == null ? Result.fail(404, "not found") : Result.ok(s);
  }

  @PostMapping("/devices/{deviceId}/run")
  public Result<AgentEnvelope> runCommand(
      @PathVariable String deviceId, @RequestBody Map<String, Object> body) {
    Long userId = currentUserId();
    if (userId == null)
      return Result.fail(401, "not logged in");
    String command = (String) body.get("command");
    if (command == null || command.isBlank()) {
      return Result.fail(400, "command required");
    }
    // Hard-coded allowlist (defense in depth: even if a malicious admin role tries to invoke
    // arbitrary command names, only whitelisted ones will be accepted here).
    if (!"TOOLCHAIN_INSPECT".equals(command)) {
      return Result.fail(400, "unknown command: " + command);
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> params = (Map<String, Object>) body.getOrDefault("params", Map.of());
    long timeoutMs = 15000;
    Object t = body.get("timeoutMs");
    if (t instanceof Number n)
      timeoutMs = n.longValue();

    return gateway.runCommand(userId, deviceId, command, params, timeoutMs);
  }

  private Long currentUserId() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
      return null;
    }
    String username = auth.getName();
    return gateway.resolveUserIdByUsername(username);
  }
}
