package com.englishword.mcptool;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MCP Tool配置属性
 */
@Data
@ConfigurationProperties(prefix = "mcp.tool")
@Component
public class McpToolProperties {

    /**
     * 是否启用 MCP Tool
     */
    private boolean enabled = true;

    /**
     * 工具端连接的 URL（用于注册工具）
     * 示例: ws://192.168.1.68:8004/mcp_endpoint/mcp/
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

    // ==================== 重连配置 ====================

    /**
     * 是否启用自动重连
     */
    private boolean autoReconnect = true;

    /**
     * 重连初始延迟（秒）
     */
    private int reconnectInitialDelay = 1;

    /**
     * 重连最大延迟（秒）
     */
    private int reconnectMaxDelay = 30;

    /**
     * 心跳检测间隔（秒）
     */
    private int heartbeatInterval = 30;

    // ==================== 工具扫描配置 ====================

    /**
     * 工具扫描配置
     */
    private ScanConfig scan = new ScanConfig();

    @Data
    public static class ScanConfig {
        /**
         * 扫描工具的包路径
         */
        private List<String> basePackages = List.of("com.englishword.tools");
    }
}
