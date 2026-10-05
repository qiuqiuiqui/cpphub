package com.cpphub.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("category")
public class Category {
  @TableId(type = IdType.AUTO) private Long id;

  private String name;

  private String slug;

  @TableField("parent_id") private Long parentId;

  private Integer sort;

  @TableField("is_enabled") private Boolean enabled;

  @TableField("created_at") private LocalDateTime createdAt;

  @TableField("updated_at") private LocalDateTime updatedAt;
}
