package com.cpphub.blog.controller;

import com.cpphub.blog.common.Result;
import com.cpphub.blog.dto.TagDTO;
import com.cpphub.blog.service.TagService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminTagController {
  private final TagService tagService;

  @GetMapping("/tags")
  public Result<List<TagDTO>> list() {
    return Result.ok(tagService.listAll());
  }

  @PostMapping("/tags")
  public Result<TagDTO> create(@RequestBody TagDTO dto) {
    return Result.ok(tagService.create(dto));
  }

  @PutMapping("/tags/{id}")
  public Result<TagDTO> update(@PathVariable Long id, @RequestBody TagDTO dto) {
    return Result.ok(tagService.update(id, dto));
  }

  @DeleteMapping("/tags/{id}")
  public Result<Void> delete(@PathVariable Long id) {
    tagService.delete(id);
    return Result.ok();
  }
}
