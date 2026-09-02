package com.cpphub.blog.module.agentgateway.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;


@Data
@TableName("agent_command_log")
public class AgentCommandLog {
  @TableId(type = IdType.AUTO) private Long id;

  private String requestId;

  private String deviceId;

  private Long userId;

  private String command;

  private String paramsJson;

  private Integer exitCode;

  private Long durationMs;

  private String status;

  private String errorMsg;

  private LocalDateTime createdAt;
}