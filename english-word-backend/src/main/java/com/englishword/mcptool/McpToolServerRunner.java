package com.englishword.mcptool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * MCP 工具服务器启动器
 *
 * 在 Spring Boot 启动时最先运行（Order=1），确保工具先注册到 MCP Server
 * 然后AI 客户端才能获取到工具列表
 */
@Slf4j
@Component
@Order(1)  // 最高优先级，确保在 McpClient 之前运行
@RequiredArgsConstructor
public class McpToolServerRunner implements ApplicationRunner {

    private final McpToolServer mcpToolServer;
    private final McpToolRegistry toolRegistry;
    private final McpToolProperties mcpToolProperties;

    @Override
    public void run(ApplicationArguments args) {
        // 检查是否启用
        if (!mcpToolProperties.isEnabled()) {
            log.info("[MCP-ToolServerRunner] MCP Tool 未启用，跳过工具服务器启动");
            return;
        }

        log.info("========================================");
        log.info("  MCP Tool Server Runner");
        log.info("========================================");
        log.info("[MCP-ToolServerRunner] 已扫描工具数量: {}", toolRegistry.getToolCount());

        // 连接到 MCP Endpoint Server
        boolean connected = mcpToolServer.connect();

        if (!connected) {
            log.warn("[MCP-ToolServerRunner] 工具服务器连接失败");
            return;
        }

        // 等待连接就绪
        if (mcpToolServer.awaitConnection(mcpToolProperties.getConnectTimeout())) {
            log.info("[MCP-ToolServerRunner] 工具服务器启动成功");
        } else {
            log.warn("[MCP-ToolServerRunner] 工具服务器连接超时");
        }

        log.info("========================================");
    }
}
