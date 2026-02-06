package com.englishword.service;

import com.englishword.dto.request.LoginRequest;
import com.englishword.dto.request.RegisterRequest;
import com.englishword.dto.response.ApiResponse;
import com.englishword.entity.User;
import com.englishword.repository.UserRepository;
import com.englishword.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 用户认证服务
 *
 * 功能：
 * - 用户注册
 * - 用户登录
 * - 生成JWT Token
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 注册结果，包含用户信息和Token
     */
    @Transactional
    public ApiResponse<Map<String, Object>> register(RegisterRequest request) {
        // 1. 验证用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            return ApiResponse.error(400, "用户名已存在");
        }

        // 2. 创建新用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());

        // 3. 保存用户
        user = userRepository.save(user);

        // 4. 生成Token
        String token = jwtUtil.generateToken(user.getUserId(), user.getUsername());

        // 5. 构建返回数据
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("token", token);

        return ApiResponse.success(data, "注册成功");
    }

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录结果，包含用户信息和Token
     */
    public ApiResponse<Map<String, Object>> login(LoginRequest request) {
        // 1. 查找用户
        Optional<User> userOptional = userRepository.findByUsername(request.getUsername());
        if (userOptional.isEmpty()) {
            return ApiResponse.error(401, "用户名或密码错误");
        }

        User user = userOptional.get();

        // 2. 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ApiResponse.error(401, "用户名或密码错误");
        }

        // 3. 生成Token
        String token = jwtUtil.generateToken(user.getUserId(), user.getUsername());

        // 4. 构建返回数据
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("avatar", user.getAvatar());
        data.put("token", token);

        return ApiResponse.success(data, "登录成功");
    }

    /**
     * 验证Token并获取用户信息
     *
     * @param token JWT Token
     * @return 用户信息
     */
    public ApiResponse<User> validateTokenAndGetUser(String token) {
        // 1. 验证Token
        if (!jwtUtil.validateToken(token)) {
            return ApiResponse.error(401, "Token无效或已过期");
        }

        // 2. 从Token中获取用户ID
        String userId = jwtUtil.getUserIdFromToken(token);

        // 3. 查询用户
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ApiResponse.error(404, "用户不存在");
        }

        User user = userOptional.get();
        return ApiResponse.success(user, "Token验证成功");
    }
}
