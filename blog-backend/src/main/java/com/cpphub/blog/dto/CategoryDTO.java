package com.cpphub.blog.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CategoryDTO {
  private Long id;
  private String name;
  private String slug;
  private Long parentId;
  private Integer depth;
  private Integer sort;
  private Boolean enabled;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
