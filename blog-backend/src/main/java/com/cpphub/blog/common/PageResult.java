package com.cpphub.blog.common;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
  private List<T> records;
  private long total;
  private long page;
  private long size;

  public static <T> PageResult<T> of(List<T> records, long total, long page, long size) {
    return new PageResult<>(records, total, page, size);
  }
}