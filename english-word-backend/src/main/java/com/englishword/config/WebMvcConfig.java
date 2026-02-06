package com.englishword.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置
 *
 * 功能：
 * - 注册JWT拦截器
 * - 配置拦截路径和白名单
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    /**
     * 注册拦截器
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")  // 拦截所有API路径
                .excludePathPatterns(
                        // 认证相关API（无需Token）
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/logout",

                        // 健康检查（无需Token）
                        "/api/health",

                        // API文档（无需Token）
                        "/api/swagger-ui.html",
                        "/api/swagger-ui/**",
                        "/api/api-docs/**",
                        "/api/swagger-resources/**",
                        "/api/v3/api-docs/**",

                        // 静态资源（无需Token）
                        "/api/static/**",
                        "/api/favicon.ico",

                        // WebSocket（有自己的认证机制）
                        "/api/ws/**"
                );
    }
}
