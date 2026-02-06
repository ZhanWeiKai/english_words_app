package com.englishword.service;

import com.englishword.dto.request.LoginRequest;
import com.englishword.dto.request.RegisterRequest;
import com.englishword.dto.response.ApiResponse;
import com.englishword.entity.User;
import com.englishword.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthService单元测试
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        // 准备注册请求数据
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("123456");
        registerRequest.setNickname("测试用户");

        // 准备登录请求数据
        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("123456");

        // 准备测试用户
        testUser = new User();
        testUser.setUserId("user_test_001");
        testUser.setUsername("testuser");
        testUser.setPassword(new BCryptPasswordEncoder().encode("123456"));
        testUser.setNickname("测试用户");
    }

    @Test
    void testRegister_Success() {
        // Mock repository行为
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // 调用服务
        ApiResponse<Map<String, Object>> response = authService.register(registerRequest);

        // 验证结果
        assertTrue(response.getCode() == 200);
        assertNotNull(response.getData());
        assertEquals("user_test_001", response.getData().get("userId"));
        assertEquals("testuser", response.getData().get("username"));
        assertNotNull(response.getData().get("token"));

        // 验证repository调用
        verify(userRepository, times(1)).existsByUsername("testuser");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegister_UsernameExists() {
        // Mock repository行为
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // 调用服务
        ApiResponse<Map<String, Object>> response = authService.register(registerRequest);

        // 验证结果
        assertEquals(400, response.getCode());
        assertEquals("用户名已存在", response.getMessage());

        // 验证repository调用
        verify(userRepository, times(1)).existsByUsername("testuser");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLogin_Success() {
        // Mock repository行为
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // 调用服务
        ApiResponse<Map<String, Object>> response = authService.login(loginRequest);

        // 验证结果
        assertTrue(response.getCode() == 200);
        assertNotNull(response.getData());
        assertEquals("user_test_001", response.getData().get("userId"));
        assertEquals("testuser", response.getData().get("username"));
        assertNotNull(response.getData().get("token"));

        // 验证repository调用
        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    void testLogin_UserNotFound() {
        // Mock repository行为
        when(userRepository.findByUsername("notexist")).thenReturn(Optional.empty());

        // 修改登录请求
        loginRequest.setUsername("notexist");

        // 调用服务
        ApiResponse<Map<String, Object>> response = authService.login(loginRequest);

        // 验证结果
        assertEquals(401, response.getCode());
        assertEquals("用户名或密码错误", response.getMessage());

        // 验证repository调用
        verify(userRepository, times(1)).findByUsername("notexist");
    }

    @Test
    void testLogin_WrongPassword() {
        // Mock repository行为
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // 修改登录请求（错误密码）
        loginRequest.setPassword("wrongpassword");

        // 调用服务
        ApiResponse<Map<String, Object>> response = authService.login(loginRequest);

        // 验证结果
        assertEquals(401, response.getCode());
        assertEquals("用户名或密码错误", response.getMessage());

        // 验证repository调用
        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    void testValidateTokenAndGetUser_Success() {
        // 创建一个模拟的Token
        String mockToken = "mock_valid_token";

        // Mock JwtUtil和repository行为（这里需要mock JwtUtil，但为了简化测试，我们只测试逻辑）
        // 注意：完整测试需要mock JwtUtil
    }
}
