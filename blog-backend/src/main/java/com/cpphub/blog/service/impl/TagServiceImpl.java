package com.cpphub.blog.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cpphub.blog.dto.TagDTO;
import com.cpphub.blog.entity.PostTag;
import com.cpphub.blog.entity.Tag;
import com.cpphub.blog.mapper.PostTagMapper;
import com.cpphub.blog.mapper.TagMapper;
import com.cpphub.blog.service.TagService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {
  private final TagMapper tagMapper;
  private final PostTagMapper postTagMapper;

  @Override
  public List<TagDTO> listAll() {
    return tagMapper.selectList(Wrappers.<Tag>lambdaQuery().orderByAsc(Tag::getName))
        .stream()
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  @Override
  public TagDTO getById(Long id) {
    Tag tag = tagMapper.selectById(id);
    return tag == null ? null : toDto(tag);
  }

  @Override
  @Transactional
  public TagDTO create(TagDTO dto) {
    String name = normalizeName(dto == null ? null : dto.getName());
    if (name.isBlank()) {
      throw new IllegalArgumentException("tag name is required");
    }
    if (tagMapper.selectOne(Wrappers.<Tag>lambdaQuery().eq(Tag::getName, name)) != null) {
      throw new IllegalArgumentException("tag already exists: " + name);
    }

    Tag tag = new Tag();
    tag.setName(name);
    tag.setColor(normalizeColor(dto == null ? null : dto.getColor()));
    tag.setUseCount(0);
    tag.setCreatedAt(LocalDateTime.now());
    tag.setUpdatedAt(LocalDateTime.now());
    tagMapper.insert(tag);
    return toDto(tag);
  }

  @Override
  @Transactional
  public TagDTO update(Long id, TagDTO dto) {
    Tag tag = tagMapper.selectById(id);
    if (tag == null) {
      throw new IllegalArgumentException("tag not found: " + id);
    }
    String nextName = normalizeName(dto == null ? null : dto.getName());
    if (!nextName.isBlank() && !Objects.equals(tag.getName(), nextName)) {
      Tag existing = tagMapper.selectOne(Wrappers.<Tag>lambdaQuery().eq(Tag::getName, nextName));
      if (existing != null && !Objects.equals(existing.getId(), id)) {
        throw new IllegalArgumentException("tag already exists: " + nextName);
      }
      tag.setName(nextName);
    }
    if (dto != null && dto.getColor() != null) {
      tag.setColor(normalizeColor(dto.getColor()));
    }
    tag.setUpdatedAt(LocalDateTime.now());
    tagMapper.updateById(tag);
    return toDto(tag);
  }

  @Override
  @Transactional
  public void delete(Long id) {
    Tag tag = tagMapper.selectById(id);
    if (tag == null) {
      return;
    }
    postTagMapper.delete(Wrappers.<PostTag>lambdaQuery().eq(PostTag::getTagId, id));
    tagMapper.deleteById(id);
  }

  @Override
  @Transactional
  public TagDTO findOrCreate(String name, String color) {
    String normalized = normalizeName(name);
    if (normalized.isBlank()) {
      throw new IllegalArgumentException("tag name is required");
    }
    Tag existing = tagMapper.selectOne(Wrappers.<Tag>lambdaQuery().eq(Tag::getName, normalized));
    if (existing != null) {
      if (existing.getColor() == null || existing.getColor().isBlank()) {
        existing.setColor(normalizeColor(color));
        existing.setUpdatedAt(LocalDateTime.now());
        tagMapper.updateById(existing);
      }
      return toDto(existing);
    }
    Tag tag = new Tag();
    tag.setName(normalized);
    tag.setColor(normalizeColor(color));
    tag.setUseCount(0);
    tag.setCreatedAt(LocalDateTime.now());
    tag.setUpdatedAt(LocalDateTime.now());
    tagMapper.insert(tag);
    return toDto(tag);
  }

  private TagDTO toDto(Tag tag) {
    TagDTO dto = new TagDTO();
    dto.setId(tag.getId());
    dto.setName(tag.getName());
    dto.setColor(tag.getColor() == null || tag.getColor().isBlank() ? "#4098FC" : tag.getColor());
    dto.setUseCount(tag.getUseCount() == null ? 0 : tag.getUseCount());
    dto.setCreatedAt(tag.getCreatedAt());
    dto.setUpdatedAt(tag.getUpdatedAt());
    return dto;
  }

  private String normalizeName(String name) {
    if (name == null) {
      return "";
    }
    return name.trim().replaceAll("\\s+", " ");
  }

  private String normalizeColor(String color) {
    if (color == null || color.isBlank()) {
      return "#4098FC";
    }
    String value = color.trim();
    return value.startsWith("#") ? value : "#" + value;
  }
}
