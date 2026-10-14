package com.cpphub.blog.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cpphub.blog.common.PageResult;
import com.cpphub.blog.dto.PostDTO;
import com.cpphub.blog.dto.PostDetailDTO;
import com.cpphub.blog.dto.PostSummaryDTO;
import com.cpphub.blog.entity.Category;
import com.cpphub.blog.entity.Post;
import com.cpphub.blog.entity.PostTag;
import com.cpphub.blog.entity.Tag;
import com.cpphub.blog.mapper.CategoryMapper;
import com.cpphub.blog.mapper.PostMapper;
import com.cpphub.blog.mapper.PostTagMapper;
import com.cpphub.blog.mapper.TagMapper;
import com.cpphub.blog.service.MarkdownService;
import com.cpphub.blog.service.PostService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {
  private final MarkdownService markdownService;
  private final TagMapper tagMapper;
  private final CategoryMapper categoryMapper;
  private final PostTagMapper postTagMapper;

  @Override
  public PageResult<PostSummaryDTO> listPublished(long page, long size, String tagName) {
    IPage<Post> p = new Page<>(page, size);
    IPage<Post> result = baseMapper.selectPublishedPage(p, tagName);
    List<PostSummaryDTO> records =
        result.getRecords().stream().map(this::toSummary).collect(Collectors.toList());
    // attach tags
    if (!records.isEmpty()) {
      Map<Long, List<String>> tagsByPost = loadTagsForPosts(
          records.stream().map(PostSummaryDTO::getId).collect(Collectors.toList()));
      records.forEach(r -> r.setTags(tagsByPost.getOrDefault(r.getId(), List.of())));
    }
    return PageResult.of(records, result.getTotal(), page, size);
  }

  @Override
  public List<PostSummaryDTO> listPublishedByCategory(Long categoryId, long page, long size) {
    List<Post> posts = baseMapper.selectList(Wrappers.<Post>lambdaQuery()
            .eq(Post::getCategoryId, categoryId)
            .eq(Post::getStatus, "PUBLISHED")
            .isNull(Post::getDeletedAt)
            .orderByDesc(Post::getPublishedAt));
    return posts.stream().map(this::toSummary).collect(Collectors.toList());
  }

  @Override
  public List<PostSummaryDTO> listPublishedByCategoryBySlug(String categorySlug) {
    if (StrUtil.isBlank(categorySlug)) {
      return List.of();
    }
    Category category = categoryMapper.selectOne(Wrappers.<Category>lambdaQuery()
            .eq(Category::getSlug, categorySlug)
            .eq(Category::getEnabled, true));
    if (category == null) {
      return List.of();
    }
    return listPublishedByCategory(category.getId(), 1, 50);
  }

  @Override
  public PageResult<PostSummaryDTO> listAdmin(long page, long size, String status) {
    IPage<Post> p = new Page<>(page, size);
    IPage<Post> result = baseMapper.selectAdminPage(p, status);
    List<PostSummaryDTO> records =
        result.getRecords().stream().map(this::toSummary).collect(Collectors.toList());
    if (!records.isEmpty()) {
      Map<Long, List<String>> tagsByPost = loadTagsForPosts(
          records.stream().map(PostSummaryDTO::getId).collect(Collectors.toList()));
      records.forEach(r -> r.setTags(tagsByPost.getOrDefault(r.getId(), List.of())));
    }
    long total = baseMapper.selectAdminCount(status);
    return PageResult.of(records, total, page, size);
  }

  @Override
  public PostDetailDTO fetchById(Long id) {
    return fetchDetail(super.getById(id));
  }

  @Override
  public PostDetailDTO getBySlug(String slug) {
    Post post = getOne(Wrappers.<Post>lambdaQuery()
            .eq(Post::getSlug, slug)
            .eq(Post::getStatus, "PUBLISHED")
            .isNull(Post::getDeletedAt));
    if (post != null) {
      post.setViewCount((post.getViewCount() == null ? 0 : post.getViewCount()) + 1);
      updateById(post);
    }
    return fetchDetail(post);
  }

  private PostDetailDTO fetchDetail(Post post) {
    if (post == null)
      return null;
    List<String> tags =
        loadTagsForPosts(List.of(post.getId())).getOrDefault(post.getId(), List.of());
    return markdownService.toDetail(post, tags);
  }

  @Override
  public List<PostSummaryDTO> search(String keyword, int limit) {
    if (StrUtil.isBlank(keyword))
      return List.of();
    List<Post> posts = baseMapper.searchByKeyword(keyword, Math.min(limit, 50));
    return posts.stream().map(this::toSummary).collect(Collectors.toList());
  }

  @Override
  @Transactional
  public PostDetailDTO create(PostDTO dto) {
    Post post = new Post();
    post.setTitle(dto.getTitle());
    post.setSlug(StrUtil.isNotBlank(dto.getSlug()) ? dto.getSlug() : generateSlug(dto.getTitle()));
    post.setContentMd(dto.getContentMd());
    post.setCategoryId(resolveCategoryId(dto.getCategoryId()));
    post.setWordCount(calculateWordCount(dto.getContentMd()));
    post.setViewCount(0);
    markdownService.renderAndAttach(post);
    post.setStatus(StrUtil.isBlank(dto.getStatus()) ? "DRAFT" : dto.getStatus().toUpperCase());
    if ("PUBLISHED".equals(post.getStatus())) {
      post.setPublishedAt(LocalDateTime.now());
    }
    save(post);
    attachTags(post.getId(), dto.getTags());
    return markdownService.toDetail(post, dto.getTags() == null ? List.of() : dto.getTags());
  }

  @Override
  @Transactional
  public PostDetailDTO update(Long id, PostDTO dto) {
    Post post = getById(id);
    if (post == null)
      throw new IllegalArgumentException("Post not found: " + id);
    post.setTitle(dto.getTitle());
    if (StrUtil.isNotBlank(dto.getSlug()) && !dto.getSlug().equals(post.getSlug())) {
      post.setSlug(dto.getSlug());
    }
    post.setContentMd(dto.getContentMd());
    post.setCategoryId(resolveCategoryId(
        dto.getCategoryId() != null ? dto.getCategoryId() : post.getCategoryId()));
    post.setWordCount(calculateWordCount(dto.getContentMd()));
    markdownService.renderAndAttach(post);
    if (StrUtil.isNotBlank(dto.getStatus())) {
      String newStatus = dto.getStatus().toUpperCase();
      if ("PUBLISHED".equals(newStatus) && !"PUBLISHED".equals(post.getStatus())) {
        post.setPublishedAt(LocalDateTime.now());
      }
      post.setStatus(newStatus);
    }
    updateById(post);
    attachTags(post.getId(), dto.getTags());
    List<String> tags = dto.getTags() == null ? List.of() : dto.getTags();
    return markdownService.toDetail(post, tags);
  }

  @Override
  @Transactional
  public void delete(Long id) {
    Set<Long> tagIds =
        postTagMapper.selectList(Wrappers.<PostTag>lambdaQuery().eq(PostTag::getPostId, id))
            .stream()
            .map(PostTag::getTagId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    removeById(id);
    postTagMapper.delete(Wrappers.<PostTag>lambdaQuery().eq(PostTag::getPostId, id));
    syncTagUseCounts(tagIds);
  }

  private PostSummaryDTO toSummary(Post post) {
    PostSummaryDTO dto = new PostSummaryDTO();
    dto.setId(post.getId());
    dto.setTitle(post.getTitle());
    dto.setSlug(post.getSlug());
    dto.setStatus(post.getStatus());
    dto.setPublishedAt(post.getPublishedAt());
    dto.setCreatedAt(post.getCreatedAt());
    dto.setUpdatedAt(post.getUpdatedAt());
    dto.setCategoryId(post.getCategoryId());
    dto.setWordCount(post.getWordCount() == null ? calculateWordCount(post.getContentMd())
                                                 : post.getWordCount());
    dto.setViewCount(post.getViewCount() == null ? 0 : post.getViewCount());
    return dto;
  }

  private int calculateWordCount(String contentMd) {
    if (StrUtil.isBlank(contentMd)) {
      return 0;
    }
    String text = contentMd.replaceAll("[`*_~>#+\\-\\[\\]()]", " ");
    String[] words = text.split("\\s+");
    int count = 0;
    for (String word : words) {
      if (!word.isBlank()) {
        count++;
      }
    }
    return count;
  }

  private Long resolveCategoryId(Long requestedCategoryId) {
    Long categoryId = requestedCategoryId;
    if (categoryId != null) {
      Category category = categoryMapper.selectById(categoryId);
      if (category != null && Boolean.TRUE.equals(category.getEnabled())) {
        return categoryId;
      }
    }
    Category fallback = categoryMapper.selectOne(Wrappers.<Category>lambdaQuery()
            .eq(Category::getSlug, "other")
            .eq(Category::getEnabled, true));
    if (fallback != null) {
      return fallback.getId();
    }
    Category other = new Category();
    other.setName("其它");
    other.setSlug("other");
    other.setParentId(null);
    other.setSort(99);
    other.setEnabled(true);
    other.setCreatedAt(LocalDateTime.now());
    other.setUpdatedAt(LocalDateTime.now());
    categoryMapper.insert(other);
    return other.getId();
  }

  private Map<Long, List<String>> loadTagsForPosts(List<Long> postIds) {
    if (postIds.isEmpty())
      return Map.of();
    List<Map<String, Object>> rows = postTagMapper.selectTagsByPostIds(postIds);
    Map<Long, List<String>> result = new HashMap<>();
    for (Map<String, Object> row : rows) {
      Long postId = ((Number) row.get("post_id")).longValue();
      String tagName = (String) row.get("name");
      result.computeIfAbsent(postId, k -> new java.util.ArrayList<>()).add(tagName);
    }
    return result;
  }

  private void attachTags(Long postId, List<String> tagNames) {
    Set<Long> oldTagIds =
        postTagMapper.selectList(Wrappers.<PostTag>lambdaQuery().eq(PostTag::getPostId, postId))
            .stream()
            .map(PostTag::getTagId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    postTagMapper.delete(Wrappers.<PostTag>lambdaQuery().eq(PostTag::getPostId, postId));
    if (tagNames == null || tagNames.isEmpty()) {
      syncTagUseCounts(oldTagIds);
      return;
    }

    Set<Long> tagIds = new LinkedHashSet<>();
    for (String raw : tagNames) {
      String name = raw == null ? "" : raw.trim();
      if (name.isEmpty())
        continue;
      Tag existing = tagMapper.selectOne(Wrappers.<Tag>lambdaQuery().eq(Tag::getName, name));
      Long tagId;
      if (existing == null) {
        Tag t = new Tag();
        t.setName(name);
        t.setColor(defaultTagColor(name));
        t.setUseCount(0);
        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedAt(LocalDateTime.now());
        tagMapper.insert(t);
        tagId = t.getId();
      } else {
        tagId = existing.getId();
        if (existing.getColor() == null || existing.getColor().isBlank()) {
          existing.setColor(defaultTagColor(name));
          existing.setUpdatedAt(LocalDateTime.now());
          tagMapper.updateById(existing);
        }
      }
      tagIds.add(tagId);
      PostTag pt = new PostTag();
      pt.setPostId(postId);
      pt.setTagId(tagId);
      postTagMapper.insert(pt);
    }
    syncTagUseCounts(tagIds);
    syncTagUseCounts(oldTagIds);
  }

  private void syncTagUseCounts(Set<Long> tagIds) {
    if (tagIds == null || tagIds.isEmpty()) {
      return;
    }
    for (Long tagId : tagIds) {
      Long count =
          postTagMapper.selectCount(Wrappers.<PostTag>lambdaQuery().eq(PostTag::getTagId, tagId));
      Tag tag = tagMapper.selectById(tagId);
      if (tag == null) {
        continue;
      }
      tag.setUseCount(Math.toIntExact(count));
      tag.setUpdatedAt(LocalDateTime.now());
      tagMapper.updateById(tag);
    }
  }

  private String defaultTagColor(String tagName) {
    String[] palette = {"#4098FC", "#18A058", "#F0A020", "#D03050", "#7D4DDB", "#2DB7F5", "#F56292",
        "#6CBF84", "#FF7A45", "#9C6ADE"};
    if (tagName == null || tagName.isBlank()) {
      return palette[0];
    }
    int index = Math.abs(tagName.hashCode()) % palette.length;
    return palette[index];
  }

  private String generateSlug(String title) {
    if (title == null)
      return "post-" + System.currentTimeMillis();
    // Simple slug: keep ascii letters/digits and dashes, lowercase.
    // For Chinese titles, append a short timestamp suffix to guarantee uniqueness.
    String ascii = title.toLowerCase()
                       .replaceAll("[^a-z0-9\\s-]", "")
                       .replaceAll("\\s+", "-")
                       .replaceAll("-+", "-");
    ascii = ascii.replaceAll("^-|-$", "");
    if (ascii.isEmpty()) {
      return "post-" + System.currentTimeMillis();
    }
    return ascii + "-" + System.currentTimeMillis() % 100000;
  }
}