package com.englishword.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.englishword.config.ChatProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MCP (Model Context Protocol) 客户端
 *
 * 通过 WebSocket 连接到 MCP Endpoint Server，支持工具调用
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ai.chat.mcp", name = "enabled", havingValue = "true")
public class McpClient {

    private final ChatProperties.McpConfig config;
    private WebSocket webSocket;
    private final OkHttpClient httpClient;
    private final AtomicInteger requestId = new AtomicInteger(0);
    private final ConcurrentHashMap<String, CompletableFuture<JSONObject>> pendingRequests = new ConcurrentHashMap<>();
    private final CountDownLatch connectionLatch = new CountDownLatch(1);
    private volatile boolean connected = false;

    public McpClient(ChatProperties chatProperties) {
        this.config = chatProperties.getMcp();
        this.httpClient = new OkHttpClient.Builder()
                .pingInterval(Duration.ofSeconds(30))
                .build();
    }

    @PostConstruct
    public void init() {
        connect();
        if (awaitConnection(config.getConnectTimeout())) {
            log.info("MCP Server 连接成功: {}", config.getServerUrl());
        } else {
            log.warn("MCP Server 连接失败，将降级为无工具模式");
        }
    }

    @PreDestroy
    public void destroy() {
        close();
        log.info("MCP Client 已关闭");
    }

    /**
     * 连接到 MCP Server
     */
    public void connect() {
        try {
            String encodedToken = URLEncoder.encode(config.getToken(), StandardCharsets.UTF_8);
            String wsUrl = config.getServerUrl() + "?token=" + encodedToken;

            log.info("[MCP] 正在连接: {}", config.getServerUrl());

            Request request = new Request.Builder()
                    .url(wsUrl)
                    .build();

            webSocket = httpClient.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    log.info("[MCP] 已连接到 MCP Server");
                    connected = true;
                    connectionLatch.countDown();
                }

                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    log.debug("[MCP] 收到消息: {}", text);
                    try {
                        JSONObject json = JSON.parseObject(text);
                        String id = json.getString("id");

                        if (id != null) {
                            CompletableFuture<JSONObject> future = pendingRequests.remove(id);
                            if (future != null) {
                                future.complete(json);
                            }
                        }
                    } catch (Exception e) {
                        log.error("[MCP] 解析消息失败: {}", e.getMessage());
                    }
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    log.error("[MCP] 连接失败: {}", t.getMessage());
                    connected = false;
                    connectionLatch.countDown();
                }

                @Override
                public void onClosing(WebSocket webSocket, int code, String reason) {
                    log.info("[MCP] 连接正在关闭: {} - {}", code, reason);
                }

                @Override
                public void onClosed(WebSocket webSocket, int code, String reason) {
                    log.info("[MCP] 连接已关闭: {} - {}", code, reason);
                    connected = false;
                }
            });
        } catch (Exception e) {
            log.error("[MCP] 创建连接异常: {}", e.getMessage());
            connected = false;
            connectionLatch.countDown();
        }
    }

    /**
     * 等待连接建立
     */
    public boolean awaitConnection(long timeoutSeconds) {
        try {
            return connectionLatch.await(timeoutSeconds, TimeUnit.SECONDS) && connected;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 获取可用工具列表
     */
    public CompletableFuture<JSONObject> listTools() {
        return sendRequest("tools/list", null);
    }

    /**
     * 调用工具
     * @param toolName 工具名称
     * @param arguments 工具参数
     */
    public CompletableFuture<JSONObject> callTool(String toolName, JSONObject arguments) {
        JSONObject params = new JSONObject();
        params.put("name", toolName);
        if (arguments != null) {
            params.put("arguments", arguments);
        }
        return sendRequest("tools/call", params);
    }

    /**
     * 发送 JSON-RPC 请求
     */
    private CompletableFuture<JSONObject> sendRequest(String method, JSONObject params) {
        String id = String.valueOf(requestId.incrementAndGet());

        JSONObject request = new JSONObject();
        request.put("jsonrpc", "2.0");
        request.put("id", id);
        request.put("method", method);
        if (params != null) {
            request.put("params", params);
        }

        CompletableFuture<JSONObject> future = new CompletableFuture<>();
        pendingRequests.put(id, future);

        String message = request.toJSONString();
        log.debug("[MCP] 发送请求: {}", message);

        if (webSocket != null) {
            webSocket.send(message);
        } else {
            future.completeExceptionally(new RuntimeException("WebSocket 未连接"));
        }

        return future;
    }

    /**
     * 关闭连接
     */
    public void close() {
        if (webSocket != null) {
            webSocket.close(1000, "Client closing");
        }
        httpClient.dispatcher().executorService().shutdown();
    }

    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return connected;
    }
}
