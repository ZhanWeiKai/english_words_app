package com.englishword.controller;

import com.englishword.dto.request.LoginRequest;
import com.englishword.dto.request.RegisterRequest;
import com.englishword.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * AuthController集成测试
 *
 * 测试完整的认证流程
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private final String testUsername = "integration_test_user";
    private final String testPassword = "123456";

    @BeforeEach
    void setUp() {
        // 清理测试数据
        userRepository.findByUsername(testUsername).ifPresent(user -> userRepository.delete(user));
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        userRepository.findByUsername(testUsername).ifPresent(user -> userRepository.delete(user));
    }

    @Test
    void testRegisterAndLogin() throws Exception {
        // 1. 测试注册
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(testUsername);
        registerRequest.setPassword(testPassword);
        registerRequest.setNickname("集成测试用户");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("注册成功"))
                .andExpect(jsonPath("$.data.username").value(testUsername))
                .andExpect(jsonPath("$.data.token").exists())
                .andDo(result -> {
                    String token = objectMapper.readTree(result.getResponse().getContentAsString())
                            .path("data").path("token").asText();
                    System.out.println("注册后Token: " + token);
                });

        // 2. 测试登录
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(testUsername);
        loginRequest.setPassword(testPassword);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("登录成功"))
                .andExpect(jsonPath("$.data.username").value(testUsername))
                .andExpect(jsonPath("$.data.token").exists())
                .andDo(result -> {
                    String token = objectMapper.readTree(result.getResponse().getContentAsString())
                            .path("data").path("token").asText();
                    System.out.println("登录后Token: " + token);
                });
    }

    @Test
    void testRegister_DuplicateUsername() throws Exception {
        // 1. 第一次注册
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(testUsername);
        registerRequest.setPassword(testPassword);
        registerRequest.setNickname("集成测试用户");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // 2. 重复注册
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户名已存在"));
    }

    @Test
    void testLogin_WrongPassword() throws Exception {
        // 1. 先注册用户
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(testUsername);
        registerRequest.setPassword(testPassword);
        registerRequest.setNickname("集成测试用户");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // 2. 使用错误密码登录
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(testUsername);
        loginRequest.setPassword("wrongpassword");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void testGetCurrentUser_WithoutToken() throws Exception {
        // 不带Token访问需要认证的接口
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未提供Token"));
    }
}
