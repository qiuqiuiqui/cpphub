package com.cpphub.blog.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;


@Data
public class PostDetailDTO {
  private Long id;
  private String title;
  private String slug;
  private String contentHtml;
  private String tocJson;
  private LocalDateTime publishedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private List<String> tags;
}