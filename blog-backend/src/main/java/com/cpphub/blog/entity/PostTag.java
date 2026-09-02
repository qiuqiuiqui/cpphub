package com.cpphub.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("post_tag")
public class PostTag {
  private Long postId;

  private Long tagId;
}