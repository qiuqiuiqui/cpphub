package com.cpphub.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cpphub.blog.entity.User;

public interface UserService extends IService<User> {
  /**
   * Find user by username.
   */
  User findByUsername(String username);
}