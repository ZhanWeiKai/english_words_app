package com.englishword.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil单元测试
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String testUserId = "user_test_123";
    private final String testUsername = "testuser";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
    }

    @Test
    void testGenerateToken() {
        // 生成Token
        String token = jwtUtil.generateToken(testUserId, testUsername);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        System.out.println("Generated Token: " + token);
    }

    @Test
    void testValidateToken_Valid() {
        // 生成Token
        String token = jwtUtil.generateToken(testUserId, testUsername);

        // 验证Token
        boolean isValid = jwtUtil.validateToken(token);

        assertTrue(isValid);
    }

    @Test
    void testValidateToken_Invalid() {
        // 使用无效Token
        String invalidToken = "invalid.token.string";

        boolean isValid = jwtUtil.validateToken(invalidToken);

        assertFalse(isValid);
    }

    @Test
    void testGetUsernameFromToken() {
        // 生成Token
        String token = jwtUtil.generateToken(testUserId, testUsername);

        // 从Token中获取用户名
        String username = jwtUtil.getUsernameFromToken(token);

        assertEquals(testUsername, username);
    }

    @Test
    void testGetUserIdFromToken() {
        // 生成Token
        String token = jwtUtil.generateToken(testUserId, testUsername);

        // 从Token中获取用户ID
        String userId = jwtUtil.getUserIdFromToken(token);

        assertEquals(testUserId, userId);
    }

    @Test
    void testGetAllClaimsFromToken() {
        // 生成Token
        String token = jwtUtil.generateToken(testUserId, testUsername);

        // 获取所有Claims
        var claims = jwtUtil.getAllClaimsFromToken(token);

        assertNotNull(claims);
        assertEquals(testUsername, claims.getSubject());
        assertEquals(testUserId, claims.get("userId"));
    }
}
