package com.cpphub.blog.controller;

import com.cpphub.blog.common.Result;
import com.cpphub.blog.dto.PostDTO;
import com.cpphub.blog.dto.PostDetailDTO;
import com.cpphub.blog.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
public class AdminPostController {
  private final PostService postService;

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