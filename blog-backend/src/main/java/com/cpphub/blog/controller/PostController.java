package com.cpphub.blog.controller;

import com.cpphub.blog.common.PageResult;
import com.cpphub.blog.common.Result;
import com.cpphub.blog.dto.PostDetailDTO;
import com.cpphub.blog.dto.PostSummaryDTO;
import com.cpphub.blog.service.PostService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PostController {
  private final PostService postService;

  @GetMapping("/posts")
  public Result<PageResult<PostSummaryDTO>> list(@RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "10") long size, @RequestParam(required = false) String tag) {
    if (page < 1)
      page = 1;
    if (size < 1 || size > 50)
      size = 10;
    return Result.ok(postService.listPublished(page, size, tag));
  }

  @GetMapping("/posts/{slug}")
  public Result<PostDetailDTO> detail(@PathVariable String slug) {
    PostDetailDTO dto = postService.getBySlug(slug);
    if (dto == null)
      return Result.fail(404, "Post not found");
    return Result.ok(dto);
  }

  @GetMapping("/posts/category/{categorySlug}")
  public Result<List<PostSummaryDTO>> postsByCategory(@PathVariable String categorySlug) {
    return Result.ok(postService.listPublishedByCategoryBySlug(categorySlug));
  }

  @GetMapping("/posts/search")
  public Result<List<PostSummaryDTO>> search(
      @RequestParam String q, @RequestParam(defaultValue = "20") int limit) {
    return Result.ok(postService.search(q, limit));
  }
}