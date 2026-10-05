package com.cpphub.blog.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class PostSummaryDTO {
  private Long id;
  private String title;
  private String slug;
  private String status;
  private LocalDateTime publishedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Long categoryId;
  private Integer wordCount;
  private Integer viewCount;
  private List<String> tags;
}