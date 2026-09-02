package com.cpphub.blog.module.agentgateway.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceStatus {
  private String deviceId;
  private String name;
  private String os;
  private String agentVersion;
  private String status; // ONLINE / OFFLINE
  private LocalDateTime lastSeenAt;
  private LocalDateTime createdAt;
}