package com.cpphub.blog.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class PostDetailDTO {
  private Long id;
  private String title;
  private String slug;
  private String contentMd; // raw markdown; null for public detail, populated for admin
  private String contentHtml;
  private String tocJson;
  private String status;
  private LocalDateTime publishedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Long categoryId;
  private Integer wordCount;
  private Integer viewCount;
  private List<String> tags;
}
