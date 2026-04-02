package com.englishword.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    @Value("${asr.upload-dir:./uploads/asr}")
    private String asrUploadDir;

    @Value("${tts.output-dir:./uploads/tts}")
    private String ttsOutputDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")  // 拦截所有路径（注意：context-path已经是/api）
                .excludePathPatterns(
                        "/auth/register",
                        "/auth/login",
                        "/auth/logout",
                        "/error",
                        "/health",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/api-docs/**",
                        "/swagger-resources/**",
                        "/v3/api-docs/**",
                        "/static/**",
                        "/favicon.ico",
                        "/ws/**",
                        "/sse",
                        "/sse/**",
                        "/asr/files/**",  // ASR音频文件不需要JWT认证，DashScope需要访问
                        "/tts/files/**"   // TTS音频文件不需要JWT认证
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 暴露 ASR 上传目录为静态资源，让 DashScope 可以通过 URL 下载音频文件
        registry.addResourceHandler("/asr/files/**")
                .addResourceLocations("file:" + asrUploadDir + "/");

        // 暴露 TTS 输出目录为静态资源，让客户端可以下载合成的音频文件
        registry.addResourceHandler("/tts/files/**")
                .addResourceLocations("file:" + ttsOutputDir + "/");
    }
}
