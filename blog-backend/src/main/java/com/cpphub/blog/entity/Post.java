package com.cpphub.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;


@Data
@TableName("post")
public class Post {
  @TableId(type = IdType.AUTO) private Long id;

  private String title;

  private String slug;

  private String contentMd;

  private String contentHtml;

  private String tocJson;

  private String status;

  private LocalDateTime publishedAt;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  @TableLogic @TableField(select = false) private LocalDateTime deletedAt;
}