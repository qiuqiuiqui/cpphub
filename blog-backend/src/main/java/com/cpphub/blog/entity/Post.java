package com.cpphub.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
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

  @TableField("category_id") private Long categoryId;

  private Integer wordCount;

  private Integer viewCount;

  private String contentHtml;

  private String tocJson;

  private String status;

  private LocalDateTime publishedAt;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  /**
   * Soft-delete timestamp. We use explicit IS NULL in queries (no @TableLogic)
   *  because the column type is DATETIME but the default value (1) doesn't match.
   */
  @TableField(select = false) private LocalDateTime deletedAt;
}