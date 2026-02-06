package com.englishword.controller;

import com.englishword.dto.request.LoginRequest;
import com.englishword.dto.request.RegisterRequest;
import com.englishword.dto.response.ApiResponse;
import com.englishword.entity.User;
import com.englishword.service.AuthService;
import com.englishword.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户认证控制器
 *
 * 提供的API：
 * - POST /api/auth/register - 用户注册
 * - POST /api/auth/login - 用户登录
 * - GET /api/auth/me - 获取当前用户信息
 * - POST /api/auth/logout - 用户登出（可选实现）
 */
@Tag(name = "用户认证", description = "用户注册、登录、Token验证")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 注册结果，包含用户信息和Token
     */
    @Operation(summary = "用户注册", description = "创建新用户账号并返回Token")
    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录结果，包含用户信息和Token
     */
    @Operation(summary = "用户登录", description = "验证用户身份并返回Token")
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * 获取当前用户信息
     *
     * @param httpRequest HTTP请求（用于从Header中获取Token）
     * @return 用户信息
     */
    @Operation(summary = "获取当前用户信息", description = "根据Token获取当前登录用户的信息")
    @GetMapping("/me")
    public ApiResponse<User> getCurrentUser(HttpServletRequest httpRequest) {
        // 从请求头中获取Token
        String token = extractTokenFromRequest(httpRequest);
        if (token == null) {
            return ApiResponse.error(401, "未提供Token");
        }

        return authService.validateTokenAndGetUser(token);
    }

    /**
     * 用户登出
     *
     * 注：由于JWT是无状态的，客户端只需删除本地Token即可
     * 服务端可以实现Token黑名单（如果需要）
     *
     * @return 登出结果
     */
    @Operation(summary = "用户登出", description = "客户端应删除本地存储的Token")
    @PostMapping("/logout")
    public ApiResponse<String> logout() {
        // JWT无状态，客户端删除Token即可
        return ApiResponse.success(null, "登出成功");
    }

    /**
     * 从请求中提取Token
     *
     * @param request HTTP请求
     * @return Token字符串，如果不存在则返回null
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
