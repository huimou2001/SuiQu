package com.suiqu.cloud.security;

import com.suiqu.cloud.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * JWT 认证过滤器
 * 负责拦截请求，解析 Token，并实现【互斥登录】逻辑
 */
@Component // 必须添加这个注解，Spring 才能自动装配它
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 从请求头获取 Token
        String token = request.getHeader("Authorization");

        // 如果 Header 中没有 Token，直接放行（交给后面的权限配置判断是否允许匿名）
        if (!StringUtils.hasText(token) || !token.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 截取真实的 JWT 字符串
        token = token.substring(7);

        try {
            // 2. 解析 Token 获取用户 ID
            String userId = JwtUtils.getUserId(token);

            // 3. 【核心：互斥登录校验】
            // 从 Redis 中获取该用户当前生效的 Token
            String redisToken = stringRedisTemplate.opsForValue().get("user:token:" + userId);

            // 如果 Redis 中没有该用户的 Token，或者与当前传入的不一致
            // 说明该账号在别处登录了（新生成的 Token 覆盖了 Redis 中的旧 Token）
            if (redisToken == null || !redisToken.equals(token)) {
                // 返回自定义错误：您的账号已在别处登录
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setCharacterEncoding("UTF-8");
                response.setContentType("application/json");
                response.getWriter().write("{\"code\": 401, \"msg\": \"账号已在别处登录，请重新登录\"}");
                return;
            }

            // 4. 如果校验通过，封装认证信息存入 Security 上下文
            // 这里的 userId 作为 Principal 存入，后续 Controller 可以通过 SecurityUtils 获取
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, new ArrayList<>());

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            // Token 解析失败（伪造或过期）
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 5. 继续执行后续过滤器
        filterChain.doFilter(request, response);
    }
}