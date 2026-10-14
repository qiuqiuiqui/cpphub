package com.cpphub.blog.controller;

import com.cpphub.blog.common.Result;
import com.cpphub.blog.dto.TagDTO;
import com.cpphub.blog.service.TagService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TagController {
  private final TagService tagService;

  @GetMapping("/tags")
  public Result<List<TagDTO>> list() {
    return Result.ok(tagService.listAll());
  }
}
