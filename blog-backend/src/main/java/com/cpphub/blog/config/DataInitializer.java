package com.cpphub.blog.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cpphub.blog.entity.User;
import com.cpphub.blog.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Boot-time initializer. Inserts default admin/admin123 if user table is empty.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;

  @Override
  public void run(String... args) {
    Long count = userMapper.selectCount(Wrappers.emptyWrapper());
    if (count == null || count == 0) {
      User admin = new User();
      admin.setUsername("admin");
      admin.setPasswordHash(passwordEncoder.encode("admin123"));
      admin.setRole("ADMIN");
      userMapper.insert(admin);
      log.info("Default admin user created (username: admin, password: admin123). Please change it "
               + "after first login.");
    } else {
      log.info("User table not empty, skip admin initialization.");
    }
  }
}