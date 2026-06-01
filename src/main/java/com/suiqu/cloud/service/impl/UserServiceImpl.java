package com.suiqu.cloud.service.impl;

import com.suiqu.cloud.entity.User;
import com.suiqu.cloud.mapper.UserMapper;
import com.suiqu.cloud.service.UserService;
import com.suiqu.cloud.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public String login(String username, String password) {
        // 1. 校验用户
        User user = userMapper.selectByUsername(username);
//        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
//            return "用户名或密码错误";
//        }
        if (user == null || password.equals(user.getPassword())==false) {
            return "用户名或密码错误";
        }


        // 2. 生成 JWT (包含用户ID)
        String token = JwtUtils.createToken(user.getId().toString());

        // 3. 【互斥登录核心】存入 Redis，Key 为 user:token:用户ID
        // 如果该用户之前在别处登录过，这里会覆盖旧 Token，导致旧 Token 校验失败
        redisTemplate.opsForValue().set("user:token:" + user.getId(), token, 2, TimeUnit.HOURS);

        return token;
    }


    @Override
    public void register(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userMapper.insert(user);
    }

    @Override
    public void logout(Long userId) {
        redisTemplate.delete("user:token:" + userId);
    }
}
