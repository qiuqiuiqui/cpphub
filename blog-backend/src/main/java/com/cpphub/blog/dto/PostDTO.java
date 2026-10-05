package com.cpphub.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class PostDTO {
  private Long id;

  @NotBlank(message = "title must not be blank")
  @Size(max = 255, message = "title max 255 chars")
  private String title;

  private String slug;

  @NotBlank(message = "contentMd must not be blank") private String contentMd;

  private Long categoryId;

  /**
   * Tag names to associate with this post. Empty for no tags.
   */
  private List<String> tags;

  /**
   * DRAFT or PUBLISHED. If null, defaults to DRAFT.
   */
  private String status;
}