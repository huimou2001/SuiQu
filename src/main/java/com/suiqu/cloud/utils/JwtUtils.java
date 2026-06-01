package com.suiqu.cloud.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

public class JwtUtils {
    // 新版要求密钥必须达到一定长度，建议使用生成器生成
    private static final Key KEY = Keys.hmacShaKeyFor("suiqu_cloud_secret_key_fixed_length_32_chars".getBytes());

    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 2;//2小时，单位是毫秒
    public static String createToken(String userId) {
        return Jwts.builder()
                .setSubject(userId)
                .setId(UUID.randomUUID().toString())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public static String getUserId(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}
