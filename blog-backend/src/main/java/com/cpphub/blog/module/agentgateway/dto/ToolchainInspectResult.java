package com.cpphub.blog.module.agentgateway.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolchainInspectResult {
  private String command;
  private String os;
  private List<ToolchainProbe> results;
}