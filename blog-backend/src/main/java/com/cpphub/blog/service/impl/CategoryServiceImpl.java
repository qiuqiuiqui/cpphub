package com.cpphub.blog.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cpphub.blog.dto.CategoryDTO;
import com.cpphub.blog.entity.Category;
import com.cpphub.blog.mapper.CategoryMapper;
import com.cpphub.blog.service.CategoryService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
  private final CategoryMapper categoryMapper;

  @Override
  public List<CategoryDTO> listAll() {
    return categoryMapper
        .selectList(Wrappers.<Category>lambdaQuery()
                .orderByAsc(Category::getParentId, Category::getSort)
                .eq(Category::getEnabled, true))
        .stream()
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  @Override
  public CategoryDTO getBySlug(String slug) {
    if (slug == null || slug.isBlank()) {
      return null;
    }
    Category category = categoryMapper.selectOne(Wrappers.<Category>lambdaQuery()
            .eq(Category::getSlug, slug)
            .eq(Category::getEnabled, true));
    return category == null ? null : toDto(category);
  }

  @Override
  @Transactional
  public CategoryDTO create(CategoryDTO dto) {
    if (dto == null || dto.getName() == null || dto.getName().isBlank()) {
      throw new IllegalArgumentException("category name is required");
    }
    Category category = new Category();
    category.setName(dto.getName().trim());
    category.setSlug(normalizeSlug(dto.getSlug(), dto.getName()));
    category.setParentId(dto.getParentId());
    category.setDepth(resolveDepth(dto.getParentId()));
    category.setSort(dto.getSort() == null ? 0 : dto.getSort());
    category.setEnabled(dto.getEnabled() == null || dto.getEnabled());
    category.setCreatedAt(LocalDateTime.now());
    category.setUpdatedAt(LocalDateTime.now());
    categoryMapper.insert(category);
    return toDto(category);
  }

  @Override
  @Transactional
  public CategoryDTO update(Long id, CategoryDTO dto) {
    Category category = categoryMapper.selectById(id);
    if (category == null) {
      throw new IllegalArgumentException("category not found: " + id);
    }
    if (dto.getName() != null && !dto.getName().isBlank()) {
      category.setName(dto.getName().trim());
    }
    if (dto.getSlug() != null && !dto.getSlug().isBlank()) {
      category.setSlug(normalizeSlug(dto.getSlug(), category.getName()));
    }
    if (dto.getParentId() != null) {
      category.setParentId(dto.getParentId());
      category.setDepth(resolveDepth(dto.getParentId()));
    }
    if (dto.getSort() != null) {
      category.setSort(dto.getSort());
    }
    if (dto.getEnabled() != null) {
      category.setEnabled(dto.getEnabled());
    }
    category.setUpdatedAt(LocalDateTime.now());
    categoryMapper.updateById(category);
    return toDto(category);
  }

  @Override
  @Transactional
  public void delete(Long id) {
    Category category = categoryMapper.selectById(id);
    if (category == null) {
      return;
    }
    category.setEnabled(false);
    category.setUpdatedAt(LocalDateTime.now());
    categoryMapper.updateById(category);
  }

  private CategoryDTO toDto(Category category) {
    CategoryDTO dto = new CategoryDTO();
    dto.setId(category.getId());
    dto.setName(category.getName());
    dto.setSlug(category.getSlug());
    dto.setParentId(category.getParentId());
    dto.setDepth(category.getDepth() == null ? 1 : category.getDepth());
    dto.setSort(category.getSort());
    dto.setEnabled(category.getEnabled() == null || category.getEnabled());
    dto.setCreatedAt(category.getCreatedAt());
    dto.setUpdatedAt(category.getUpdatedAt());
    return dto;
  }

  private int resolveDepth(Long parentId) {
    if (parentId == null) {
      return 1;
    }
    Category parent = categoryMapper.selectById(parentId);
    if (parent == null) {
      throw new IllegalArgumentException("parent category not found: " + parentId);
    }
    Integer parentDepth = parent.getDepth() == null ? 1 : parent.getDepth();
    if (parentDepth >= 3) {
      throw new IllegalArgumentException("category depth cannot exceed 3");
    }
    return parentDepth + 1;
  }

  private String normalizeSlug(String rawSlug, String name) {
    String base = (rawSlug == null || rawSlug.isBlank()) ? name : rawSlug;
    String slug = base.toLowerCase().trim();
    slug = slug.replaceAll("[^a-z0-9\\-\\u4e00-\\u9fff]+", "-");
    slug = slug.replaceAll("-+", "-");
    slug = slug.replaceAll("^-|-$", "");
    if (slug.isBlank()) {
      slug = "category" + System.currentTimeMillis();
    }
    return slug;
  }

  public Long ensureDefaultCategoryId() {
    Category category = categoryMapper.selectOne(Wrappers.<Category>lambdaQuery()
            .eq(Category::getSlug, "other")
            .eq(Category::getEnabled, true));
    if (category != null) {
      return category.getId();
    }
    Category other = new Category();
    other.setName("其它");
    other.setSlug("other");
    other.setParentId(null);
    other.setDepth(1);
    other.setSort(99);
    other.setEnabled(true);
    other.setCreatedAt(LocalDateTime.now());
    other.setUpdatedAt(LocalDateTime.now());
    categoryMapper.insert(other);
    return other.getId();
  }
}
