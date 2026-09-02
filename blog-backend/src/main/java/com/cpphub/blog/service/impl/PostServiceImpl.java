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
import com.cpphub.blog.entity.Post;
import com.cpphub.blog.entity.PostTag;
import com.cpphub.blog.entity.Tag;
import com.cpphub.blog.mapper.PostMapper;
import com.cpphub.blog.mapper.PostTagMapper;
import com.cpphub.blog.mapper.TagMapper;
import com.cpphub.blog.service.MarkdownService;
import com.cpphub.blog.service.PostService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  public PostDetailDTO getBySlug(String slug) {
    Post post = getOne(
        Wrappers.<Post>lambdaQuery().eq(Post::getSlug, slug).eq(Post::getStatus, "PUBLISHED"));
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
    removeById(id);
    postTagMapper.delete(Wrappers.<PostTag>lambdaQuery().eq(PostTag::getPostId, id));
  }

  private PostSummaryDTO toSummary(Post post) {
    PostSummaryDTO dto = new PostSummaryDTO();
    dto.setId(post.getId());
    dto.setTitle(post.getTitle());
    dto.setSlug(post.getSlug());
    dto.setStatus(post.getStatus());
    dto.setPublishedAt(post.getPublishedAt());
    dto.setCreatedAt(post.getCreatedAt());
    return dto;
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
    // detach existing
    postTagMapper.delete(Wrappers.<PostTag>lambdaQuery().eq(PostTag::getPostId, postId));
    if (tagNames == null || tagNames.isEmpty())
      return;
    // upsert tags
    for (String raw : tagNames) {
      String name = raw.trim();
      if (name.isEmpty())
        continue;
      Tag existing = tagMapper.selectOne(Wrappers.<Tag>lambdaQuery().eq(Tag::getName, name));
      Long tagId;
      if (existing == null) {
        Tag t = new Tag();
        t.setName(name);
        t.setUseCount(0);
        tagMapper.insert(t);
        tagId = t.getId();
      } else {
        tagId = existing.getId();
      }
      PostTag pt = new PostTag();
      pt.setPostId(postId);
      pt.setTagId(tagId);
      postTagMapper.insert(pt);
    }
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