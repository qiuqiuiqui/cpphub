package com.cpphub.blog.module.agentgateway.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;


@Data
@TableName("agent_device")
public class AgentDevice {
  @TableId(type = IdType.AUTO) private Long id;

  private String deviceId;

  private Long userId;

  private String name;

  private String os;

  private String arch;

  private String agentVersion;

  private String jwtHash;

  private String capabilities;

  private String status;

  private LocalDateTime lastSeenAt;

  private LocalDateTime createdAt;
}