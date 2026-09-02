package com.cpphub.blog.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cpphub.blog.entity.User;
import com.cpphub.blog.mapper.UserMapper;
import com.cpphub.blog.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
  @Override
  public User findByUsername(String username) {
    return getOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
  }
}