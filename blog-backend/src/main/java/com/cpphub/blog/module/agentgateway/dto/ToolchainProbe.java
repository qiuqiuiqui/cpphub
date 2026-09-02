package com.cpphub.blog.module.agentgateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolchainProbe {
  private String name;
  private Boolean available;
  private String version;
  private String path;
  private String error;
}