package com.cpphub.blog.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
  @Bean
  public OpenAPI blogOpenAPI() {
    return new OpenAPI().info(new Info()
            .title("C++ Hub Blog API")
            .version("0.1.0")
            .description("Personal C++ learning blog + local tool integration backend"));
  }
}