package com.cpphub.blog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDTO {
  @NotBlank(message = "username must not be blank") private String username;

  @NotBlank(message = "password must not be blank") private String password;
}