package com.cpphub.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cpphub.blog.entity.PostTag;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


@Mapper
public interface PostTagMapper extends BaseMapper<PostTag> {
  /**
   * Returns rows of {post_id, tag_name} for the given posts. Used to attach tags without N+1.
   */
  List<Map<String, Object>> selectTagsByPostIds(@Param("postIds") List<Long> postIds);
}