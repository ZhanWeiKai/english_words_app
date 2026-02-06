package com.englishword.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 应用配置
 */
@Data
@Component
public class AppConfig {

    @Value("${zhipuai.api-key}")
    private String zhipuAiApiKey;

    @Value("${zhipuai.api-url}")
    private String zhipuAiApiUrl;

    @Value("${zhipuai.model}")
    private String zhipuAiModel;

    @Value("${zhipuai.base-prompt}")
    private String zhipuAiBasePrompt;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;
}
