package com.cpphub.blog.module.agentgateway.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentEnvelope {
  private String type; // HELLO / PING / PONG / CMD_REQUEST / CMD_RESPONSE
  private String id; // request id (for CMD_REQUEST/CMD_RESPONSE)
  private String command; // command name
  private Map<String, Object> params;
  private Object result;
  private String error;
  private Long ts;
}