package com.suiqu.cloud.controller;

import com.suiqu.cloud.entity.Result;
import com.suiqu.cloud.entity.User;
import com.suiqu.cloud.entity.dto.LoginRequest;
import com.suiqu.cloud.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result login(@RequestBody LoginRequest req) {
        String token = userService.login(req.getUsername(), req.getPassword());
        return Result.success(token);
    }

    @PostMapping("/register")
    public Result register(@RequestBody User user) {
        userService.register(user);
        return Result.success("ok");
    }
}
