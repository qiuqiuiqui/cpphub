package com.cpphub.blog.module.agentgateway.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;


@Data
@TableName("agent_pairing_code")
public class AgentPairingCode {
  @TableId(type = IdType.ASSIGN_ID) private Long id;

  private String code;

  private Long userId;

  private String deviceName;

  private LocalDateTime expiresAt;

  private LocalDateTime usedAt;

  private LocalDateTime createdAt;
}