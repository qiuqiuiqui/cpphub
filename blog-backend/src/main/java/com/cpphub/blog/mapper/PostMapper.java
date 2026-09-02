package com.cpphub.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cpphub.blog.entity.Post;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


@Mapper
public interface PostMapper extends BaseMapper<Post> {
  /**
   * Paginate published posts, optionally filtered by tag name.
   */
  IPage<Post> selectPublishedPage(IPage<Post> page, @Param("tagName") String tagName);

  /**
   * Search posts via FULLTEXT ngram index.
   */
  List<Post> searchByKeyword(@Param("keyword") String keyword, @Param("limit") int limit);
}