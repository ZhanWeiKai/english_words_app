package com.englishword.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 *
 * 功能：
 * - 生成JWT Token
 * - 验证Token有效性
 * - 从Token中提取用户信息
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;

    /**
     * Token过期时间（7天）
     */
    @Value("${jwt.expiration:604800000}")
    private long expirationTime;

    public JwtUtil() {
        // 使用固定密钥（从application.yml读取）
        this.secretKey = Keys.hmacShaKeyFor(
            "EnglishWordAppSecretKey2026ForJWTTokenGenerationMustBeLongEnough".getBytes()
        );
    }

    /**
     * 生成Token
     *
     * @param userId 用户ID (UUID)
     * @param username 用户名
     * @return JWT Token字符串
     */
    public String generateToken(String userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        return createToken(claims, username);
    }

    /**
     * 创建Token
     *
     * @param claims 自定义声明
     * @param subject 主题（用户名）
     * @return JWT Token字符串
     */
    private String createToken(Map<String, Object> claims, String subject) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationTime))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 验证Token是否有效
     *
     * @param token JWT Token
     * @return true-有效，false-无效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从Token中获取用户名
     *
     * @param token JWT Token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    /**
     * 从Token中获取用户ID
     *
     * @param token JWT Token
     * @return 用户ID (UUID字符串)
     */
    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("userId", String.class);
    }

    /**
     * 从Token中获取所有Claims
     *
     * @param token JWT Token
     * @return Claims对象
     */
    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
