package com.cpphub.blog.controller;

import com.cpphub.blog.common.Result;
import com.cpphub.blog.dto.CategoryDTO;
import com.cpphub.blog.service.CategoryService;
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
public class AdminCategoryController {
  private final CategoryService categoryService;

  @GetMapping("/categories")
  public Result<List<CategoryDTO>> list() {
    return Result.ok(categoryService.listAll());
  }

  @PostMapping("/categories")
  public Result<CategoryDTO> create(@RequestBody CategoryDTO dto) {
    return Result.ok(categoryService.create(dto));
  }

  @PutMapping("/categories/{id}")
  public Result<CategoryDTO> update(@PathVariable Long id, @RequestBody CategoryDTO dto) {
    return Result.ok(categoryService.update(id, dto));
  }

  @DeleteMapping("/categories/{id}")
  public Result<Void> delete(@PathVariable Long id) {
    categoryService.delete(id);
    return Result.ok();
  }
}
