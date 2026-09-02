package com.cpphub.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cpphub.blog.common.PageResult;
import com.cpphub.blog.dto.PostDTO;
import com.cpphub.blog.dto.PostDetailDTO;
import com.cpphub.blog.dto.PostSummaryDTO;
import com.cpphub.blog.entity.Post;
import java.util.List;


public interface PostService extends IService<Post> {
  PageResult<PostSummaryDTO> listPublished(long page, long size, String tagName);

  PostDetailDTO getBySlug(String slug);

  List<PostSummaryDTO> search(String keyword, int limit);

  PostDetailDTO create(PostDTO dto);

  PostDetailDTO update(Long id, PostDTO dto);

  void delete(Long id);
}