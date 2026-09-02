package com.cpphub.blog.service;

import com.cpphub.blog.dto.PostDetailDTO;
import com.cpphub.blog.entity.Post;

public interface MarkdownService {
  /**
   * Render Markdown to HTML and build TOC. Result is stored in the given Post.
   */
  void renderAndAttach(Post post);

  /**
   * Build a PostDetailDTO from a Post entity (with tags).
   */
  PostDetailDTO toDetail(Post post, java.util.List<String> tags);
}