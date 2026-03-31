package com.englishword.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI Chat 配置属性
 *
 * 支持通过配置文件切换不同的 OpenAI 兼容 API：
 * - 智谱 AI (glm-4-flash)
 * - 阿里通义 (qwen-plus)
 * - DeepSeek (deepseek-chat)
 * - OpenAI (gpt-4, gpt-3.5-turbo)
 */
@Data
@ConfigurationProperties(prefix = "ai.chat")
@Component
public class ChatProperties {

    /**
     * 是否启用 AI 聊天
     */
    private boolean enabled = true;

    /**
     * API 地址（OpenAI 兼容格式）
     * 智谱: https://open.bigmodel.cn/api/paas/v4/chat/completions
     * 通义: https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
     * DeepSeek: https://api.deepseek.com/v1/chat/completions
     */
    private String apiUrl;

    /**
     * API Key（支持环境变量注入）
     */
    private String apiKey;

    /**
     * 模型名称
     */
    private String model = "glm-4-flash";

    /**
     * 请求超时时间（秒）
     */
    private int timeout = 60;

    /**
     * 温度参数 (0-1)
     */
    private double temperature = 0.7;

    /**
     * 最大返回 token 数
     */
    private int maxTokens = 2000;

    /**
     * MCP (Model Context Protocol) 配置
     */
    private McpConfig mcp = new McpConfig();

    /**
     * MCP 配置类
     */
    @Data
    public static class McpConfig {
        /**
         * 是否启用 MCP 工具调用
         */
        private boolean enabled = false;

        /**
         * MCP Endpoint Server URL (WebSocket)
         */
        private String serverUrl;

        /**
         * MCP 认证 Token
         */
        private String token;

        /**
         * 连接超时时间（秒）
         */
        private int connectTimeout = 10;
    }
}
