package com.englishword.config;

import com.englishword.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT拦截器
 *
 * 功能：
 * - 拦截需要认证的请求
 * - 验证JWT Token
 * - 将用户信息注入到请求属性中
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    /**
     * 在请求处理之前进行拦截
     *
     * @param request HTTP请求
     * @param response HTTP响应
     * @param handler 处理器
     * @return true-继续处理，false-中断请求
     * @throws Exception 异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 处理跨域预检请求
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        // 从请求头中获取Token
        String token = extractToken(request);

        // 验证Token
        if (token != null && jwtUtil.validateToken(token)) {
            // Token有效，将用户信息注入到请求属性中
            String userId = jwtUtil.getUserIdFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);

            request.setAttribute("userId", userId);
            request.setAttribute("username", username);
            return true;
        }

        // Token无效或未提供
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"未授权或Token无效\",\"data\":null}");
        return false;
    }

    /**
     * 从请求头中提取Token
     *
     * @param request HTTP请求
     * @return Token字符串，如果不存在则返回null
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
