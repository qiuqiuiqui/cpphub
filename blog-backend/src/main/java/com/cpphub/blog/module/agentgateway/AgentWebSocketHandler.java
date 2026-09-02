package com.cpphub.blog.module.agentgateway;

import com.cpphub.blog.module.agentgateway.dto.AgentEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;


@Slf4j
@Component
@RequiredArgsConstructor
public class AgentWebSocketHandler extends TextWebSocketHandler {
  private final GatewayService gateway;
  private final ObjectMapper objectMapper;

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    URI uri = session.getUri();
    if (uri == null) {
      session.close(CloseStatus.POLICY_VIOLATION);
      return;
    }
    // Auth: token comes from query param ?token=... (small MVP; production should use subprotocol)
    String token = UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("token");
    if (token == null) {
      // fallback: Authorization header (set by HttpRequest)
      var auth = session.getHandshakeHeaders().getFirst("Authorization");
      if (auth != null && auth.startsWith("Bearer "))
        token = auth.substring(7);
    }
    if (token == null) {
      session.close(CloseStatus.NOT_ACCEPTABLE);
      return;
    }
    JwtUtil.Parsed claims = JwtUtil.parse(token);
    if (claims == null || claims.deviceId == null) {
      session.close(CloseStatus.NOT_ACCEPTABLE);
      return;
    }
    // Stash device id in session attributes
    session.getAttributes().put("deviceId", claims.deviceId);
    gateway.onAgentConnected(session, claims.deviceId);
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    Object deviceIdObj = session.getAttributes().get("deviceId");
    if (deviceIdObj == null) {
      session.close(CloseStatus.POLICY_VIOLATION);
      return;
    }
    String deviceId = (String) deviceIdObj;
    gateway.touchLastSeen(deviceId);

    AgentEnvelope env = objectMapper.readValue(message.getPayload(), AgentEnvelope.class);
    if (env == null || env.getType() == null)
      return;

    switch (env.getType()) {
      case "HELLO":
        log.info("[agent {}] HELLO received", deviceId);
        // Send a CMD_RESPONSE-shaped envelope is not needed; just log.
        break;
      case "PONG":
        log.debug("[agent {}] PONG", deviceId);
        break;
      case "CMD_RESPONSE":
        gateway.deliverResponse(env);
        break;
      default:
        log.warn("[agent {}] unexpected type {}", deviceId, env.getType());
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    Object deviceIdObj = session.getAttributes().get("deviceId");
    if (deviceIdObj instanceof String deviceId) {
      gateway.onAgentDisconnected(deviceId);
    }
  }
}
