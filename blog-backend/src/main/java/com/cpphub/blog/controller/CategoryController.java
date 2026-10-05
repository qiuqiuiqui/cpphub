package com.cpphub.blog.controller;

import com.cpphub.blog.common.Result;
import com.cpphub.blog.dto.CategoryDTO;
import com.cpphub.blog.dto.PostSummaryDTO;
import com.cpphub.blog.service.CategoryService;
import com.cpphub.blog.service.PostService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CategoryController {
  private final CategoryService categoryService;
  private final PostService postService;

  @GetMapping("/categories")
  public Result<List<CategoryDTO>> listCategories() {
    return Result.ok(categoryService.listAll());
  }

  @GetMapping("/categories/{slug}")
  public Result<CategoryDTO> detail(@PathVariable String slug) {
    CategoryDTO dto = categoryService.getBySlug(slug);
    if (dto == null) {
      return Result.fail(404, "Category not found");
    }
    return Result.ok(dto);
  }

  @GetMapping("/categories/{slug}/posts")
  public Result<List<PostSummaryDTO>> postsByCategory(@PathVariable String slug) {
    CategoryDTO category = categoryService.getBySlug(slug);
    if (category == null) {
      return Result.fail(404, "Category not found");
    }
    return Result.ok(postService.listPublishedByCategory(category.getId(), 1, 50));
  }
}
