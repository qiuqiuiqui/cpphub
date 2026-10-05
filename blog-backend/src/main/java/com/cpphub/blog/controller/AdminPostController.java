package com.cpphub.blog.controller;

import com.cpphub.blog.common.PageResult;
import com.cpphub.blog.common.Result;
import com.cpphub.blog.dto.PostDTO;
import com.cpphub.blog.dto.PostDetailDTO;
import com.cpphub.blog.dto.PostSummaryDTO;
import com.cpphub.blog.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
public class AdminPostController {
  private final PostService postService;

  /**
   * Admin post list with optional status filter.
   *
   * @param status null/empty = ALL; otherwise DRAFT or PUBLISHED
   */
  @GetMapping
  public Result<PageResult<PostSummaryDTO>> list(@RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size, @RequestParam(required = false) String status) {
    if (page < 1)
      page = 1;
    if (size < 1 || size > 100)
      size = 20;
    return Result.ok(postService.listAdmin(page, size, status));
  }

  /** Admin-only: fetch a single post by id (works for drafts). */
  @GetMapping("/{id}")
  public Result<PostDetailDTO> get(@PathVariable Long id) {
    PostDetailDTO dto = postService.fetchById(id);
    if (dto == null)
      return Result.fail(404, "Post not found");
    return Result.ok(dto);
  }

  @PostMapping
  public Result<PostDetailDTO> create(@Valid @RequestBody PostDTO dto) {
    return Result.ok(postService.create(dto));
  }

  @PutMapping("/{id}")
  public Result<PostDetailDTO> update(@PathVariable Long id, @Valid @RequestBody PostDTO dto) {
    return Result.ok(postService.update(id, dto));
  }

  @DeleteMapping("/{id}")
  public Result<Void> delete(@PathVariable Long id) {
    postService.delete(id);
    return Result.ok();
  }
}
