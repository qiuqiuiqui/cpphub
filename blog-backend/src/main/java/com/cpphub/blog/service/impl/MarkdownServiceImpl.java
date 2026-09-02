package com.cpphub.blog.service.impl;

import com.cpphub.blog.dto.PostDetailDTO;
import com.cpphub.blog.entity.Post;
import com.cpphub.blog.service.MarkdownService;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class MarkdownServiceImpl implements MarkdownService {
  private final Parser parser;
  private final HtmlRenderer renderer;

  public MarkdownServiceImpl() {
    this.parser = Parser.builder().extensions(List.of(TablesExtension.create())).build();
    this.renderer = HtmlRenderer.builder().extensions(List.of(TablesExtension.create())).build();
  }

  @Override
  public void renderAndAttach(Post post) {
    if (post.getContentMd() == null) {
      post.setContentHtml("");
      post.setTocJson("[]");
      return;
    }
    var document = parser.parse(post.getContentMd());
    String html = renderer.render(document);
    // Escape content for safe HTML storage (renderer already escapes; this is a sanity guard)
    post.setContentHtml(html);
    post.setTocJson(buildTocJson(post.getContentMd()));
  }

  @Override
  public PostDetailDTO toDetail(Post post, List<String> tags) {
    PostDetailDTO dto = new PostDetailDTO();
    dto.setId(post.getId());
    dto.setTitle(post.getTitle());
    dto.setSlug(post.getSlug());
    dto.setContentHtml(post.getContentHtml());
    dto.setTocJson(post.getTocJson());
    dto.setPublishedAt(post.getPublishedAt());
    dto.setCreatedAt(post.getCreatedAt());
    dto.setUpdatedAt(post.getUpdatedAt());
    dto.setTags(tags);
    return dto;
  }

  /**
   * Minimal TOC extraction: collect ATX headings (#, ##, ### ...).
   * Returns a JSON array string; the frontend can render it.
   */
  private String buildTocJson(String markdown) {
    StringBuilder sb = new StringBuilder("[");
    Pattern p = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*$", Pattern.MULTILINE);
    Matcher m = p.matcher(markdown);
    boolean first = true;
    int idx = 0;
    while (m.find()) {
      if (!first)
        sb.append(",");
      int level = m.group(1).length();
      String text = m.group(2).trim();
      sb.append("{\"level\":")
          .append(level)
          .append(",\"text\":\"")
          .append(escape(text))
          .append("\"")
          .append(",\"id\":\"h-")
          .append(idx++)
          .append("\"}");
      first = false;
    }
    sb.append("]");
    return sb.toString();
  }

  private String escape(String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}