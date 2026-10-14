package com.cpphub.blog.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TagDTO {
  private Long id;
  private String name;
  private String color;
  private Integer useCount;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
