package com.englishword.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.englishword.config.ChatProperties;
import com.englishword.mcptool.McpConnectionEvent;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MCP (Model Context Protocol) 客户端
 *
 * 通过 WebSocket 连接到 MCP Endpoint Server，支持工具调用
 * 支持自动重连（监听工具端连接事件）
 *
 * 使用 ApplicationRunner 确保在 McpToolServer 注册工具之后再连接
 */
@Slf4j
@Component
@Order(2)  // 比 McpToolServerRunner (@Order(1)) 更晚执行
@ConditionalOnProperty(prefix = "ai.chat.mcp", name = "enabled", havingValue = "true")
public class McpClient implements ApplicationRunner {

    private final ChatProperties.McpConfig config;
    private WebSocket webSocket;
    private final OkHttpClient httpClient;
    private final AtomicInteger requestId = new AtomicInteger(0);
    private final ConcurrentHashMap<String, CompletableFuture<JSONObject>> pendingRequests = new ConcurrentHashMap<>();
    private volatile CountDownLatch connectionLatch = new CountDownLatch(1);
    private volatile boolean connected = false;

    // ==================== 重连相关 ====================
    private ScheduledExecutorService reconnectScheduler;
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    private volatile boolean waitingForToolServer = false;

    public McpClient(ChatProperties chatProperties) {
        this.config = chatProperties.getMcp();
        this.httpClient = new OkHttpClient.Builder()
                .pingInterval(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public void run(ApplicationArguments args) {
        // 在 McpToolServerRunner 完成工具注册后再连接
        log.info("[MCP-Client] 等待工具服务器就绪后连接...");
        connect();
        if (awaitConnection(config.getConnectTimeout())) {
            log.info("[MCP-Client] MCP Server 连接成功: {}", config.getServerUrl());
        } else {
            log.warn("[MCP-Client] MCP Server 连接失败，等待工具端连接事件触发重连");
        }
    }

    /**
     * 监听工具端连接事件
     * 当工具端重新连接后，客户端也重新连接以获取最新工具列表
     */
    @EventListener
    public void onMcpConnectionEvent(McpConnectionEvent event) {
        if (event.getEventType() == McpConnectionEvent.Type.TOOL_SERVER_CONNECTED) {
            log.info("[MCP-Client] 收到工具端连接事件: {}", event.getMessage());

            if (!connected && config.isAutoReconnect()) {
                scheduleReconnectAfterToolServerReady();
            }
        } else if (event.getEventType() == McpConnectionEvent.Type.TOOL_SERVER_DISCONNECTED) {
            log.info("[MCP-Client] 收到工具端断开事件: {}", event.getMessage());
            waitingForToolServer = true;
        }
    }

    /**
     * 在工具端就绪后延迟重连
     */
    private void scheduleReconnectAfterToolServerReady() {
        if (!reconnecting.compareAndSet(false, true)) {
            log.debug("[MCP-Client] 已有重连任务在进行，跳过");
            return;
        }

        if (reconnectScheduler == null || reconnectScheduler.isShutdown()) {
            reconnectScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "mcp-client-reconnect");
                t.setDaemon(true);
                return t;
            });
        }

        int delay = config.getReconnectDelay();
        log.info("[MCP-Client] 将在 {} 秒后重连（等待工具端完成注册）", delay);

        reconnectScheduler.schedule(() -> {
            try {
                waitingForToolServer = false;
                connect();

                if (awaitConnection(config.getConnectTimeout())) {
                    log.info("[MCP-Client] 重连成功");
                    // 刷新工具列表
                    refreshToolList();
                } else {
                    log.warn("[MCP-Client] 重连失败");
                }
            } catch (Exception e) {
                log.error("[MCP-Client] 重连异常: {}", e.getMessage());
            } finally {
                reconnecting.set(false);
            }
        }, delay, TimeUnit.SECONDS);
    }

    /**
     * 刷新工具列表
     */
    private void refreshToolList() {
        try {
            listTools().get(5, TimeUnit.SECONDS);
            log.info("[MCP-Client] 工具列表已刷新");
        } catch (Exception e) {
            log.warn("[MCP-Client] 刷新工具列表失败: {}", e.getMessage());
        }
    }

    /**
     * 连接到 MCP Server
     */
    public synchronized void connect() {
        try {
            // 关闭旧连接
            if (webSocket != null) {
                try {
                    webSocket.close(1000, "Reconnecting");
                } catch (Exception e) {
                    log.debug("[MCP-Client] 关闭旧连接时出错: {}", e.getMessage());
                }
                webSocket = null;
            }

            // 重置连接状态
            connectionLatch = new CountDownLatch(1);
            connected = false;

            String encodedToken = URLEncoder.encode(config.getToken(), StandardCharsets.UTF_8);
            String wsUrl = config.getServerUrl() + "?token=" + encodedToken;

            log.info("[MCP-Client] 正在连接: {}", config.getServerUrl());

            Request request = new Request.Builder()
                    .url(wsUrl)
                    .build();

            webSocket = httpClient.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket ws, Response response) {
                    log.info("[MCP-Client] 已连接到 MCP Server");
                    connected = true;
                    connectionLatch.countDown();
                }

                @Override
                public void onMessage(WebSocket ws, String text) {
                    log.debug("[MCP-Client] 收到消息: {}", text);
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
                        log.error("[MCP-Client] 解析消息失败: {}", e.getMessage());
                    }
                }

                @Override
                public void onFailure(WebSocket ws, Throwable t, Response response) {
                    log.error("[MCP-Client] 连接失败: {}", t.getMessage());
                    connected = false;
                    connectionLatch.countDown();
                }

                @Override
                public void onClosing(WebSocket ws, int code, String reason) {
                    log.info("[MCP-Client] 连接正在关闭: {} - {}", code, reason);
                }

                @Override
                public void onClosed(WebSocket ws, int code, String reason) {
                    log.info("[MCP-Client] 连接已关闭: {} - {}", code, reason);
                    connected = false;
                }
            });
        } catch (Exception e) {
            log.error("[MCP-Client] 创建连接异常: {}", e.getMessage());
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
        log.debug("[MCP-Client] 发送请求: {}", message);

        if (webSocket != null && connected) {
            webSocket.send(message);
        } else {
            future.completeExceptionally(new RuntimeException("WebSocket 未连接"));
        }

        return future;
    }

    /**
     * 关闭连接
     */
    @PreDestroy
    public void close() {
        log.info("[MCP-Client] 正在关闭...");

        // 停止重连调度器
        if (reconnectScheduler != null && !reconnectScheduler.isShutdown()) {
            reconnectScheduler.shutdown();
            try {
                if (!reconnectScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    reconnectScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                reconnectScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // 关闭 WebSocket
        if (webSocket != null) {
            webSocket.close(1000, "Client closing");
        }

        httpClient.dispatcher().executorService().shutdown();
        log.info("[MCP-Client] 已关闭");
    }

    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return connected;
    }
}
