package com.cpphub.blog.service;

import com.cpphub.blog.dto.TagDTO;
import java.util.List;

public interface TagService {
  List<TagDTO> listAll();

  TagDTO getById(Long id);

  TagDTO create(TagDTO dto);

  TagDTO update(Long id, TagDTO dto);

  void delete(Long id);

  TagDTO findOrCreate(String name, String color);
}
