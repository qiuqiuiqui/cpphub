package com.cpphub.blog.module.agentgateway;

import com.cpphub.blog.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


/**
 * Public endpoint for the local agent to redeem a pairing code.
 * No authentication on this endpoint itself - the secrecy of the 6-digit code is the auth.
 */
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentPairController {
  private final GatewayService gateway;
  private final ObjectMapper objectMapper;

  @PostMapping("/pair")
  public Result<GatewayService.PairResult> pair(@RequestBody Map<String, String> body) {
    String code = body == null ? null : body.get("code");
    String os = body == null ? null : body.getOrDefault("os", "windows");
    String arch = body == null ? null : body.getOrDefault("arch", "amd64");
    String agentVersion = body == null ? null : body.get("agentVersion");
    if (code == null || code.length() != 6) {
      return Result.fail(400, "code must be 6 digits");
    }
    return gateway.redeemCode(code, os, arch, agentVersion);
  }
}
