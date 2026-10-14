package com.cpphub.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("tag")
public class Tag {
  @TableId(type = IdType.AUTO) private Long id;

  private String name;

  private String color;

  private Integer useCount;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;
}