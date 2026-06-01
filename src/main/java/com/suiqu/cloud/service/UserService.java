package com.suiqu.cloud.service;

import com.suiqu.cloud.entity.User;

public interface UserService {
    String login(String username, String password);
    void register(User user);
    void logout(Long userId);
}
