package com.englishword.mcptool;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * MCP 工具服务器
 *
 * 连接到 MCP Endpoint Server，注册工具并处理工具调用请求
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpToolServer {

    private final McpToolProperties properties;
    private final McpToolRegistry toolRegistry;

    private WebSocket webSocket;
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .pingInterval(Duration.ofSeconds(30))
            .build();

    private final CountDownLatch connectionLatch = new CountDownLatch(1);
    private volatile boolean connected = false;

    /**
     * 连接到 MCP Endpoint Server
     */
    public boolean connect() {
        if (!properties.isEnabled()) {
            log.info("[MCP-ToolServer] MCP Tool 未启用");
            return false;
        }

        String serverUrl = properties.getServerUrl();
        if (serverUrl == null || serverUrl.isEmpty()) {
            log.warn("[MCP-ToolServer] 未配置 server-url");
            return false;
        }

        try {
            String encodedToken = URLEncoder.encode(properties.getToken(), StandardCharsets.UTF_8);
            String wsUrl = serverUrl + "?token=" + encodedToken;

            log.info("[MCP-ToolServer] 正在连接: {}", serverUrl);
            log.info("[MCP-ToolServer] 已注册工具数量: {}", toolRegistry.getToolCount());

            Request request = new Request.Builder()
                    .url(wsUrl)
                    .build();

            webSocket = httpClient.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    log.info("[MCP-ToolServer] 已连接到 MCP Endpoint Server");
                    connected = true;
                    connectionLatch.countDown();
                }

                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    log.debug("[MCP-ToolServer] 收到请求: {}", text);
                    handleMessage(text);
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    log.error("[MCP-ToolServer] 连接失败: {}", t.getMessage());
                    connected = false;
                    connectionLatch.countDown();
                }

                @Override
                public void onClosing(WebSocket webSocket, int code, String reason) {
                    log.info("[MCP-ToolServer] 连接正在关闭: {} - {}", code, reason);
                }

                @Override
                public void onClosed(WebSocket webSocket, int code, String reason) {
                    log.info("[MCP-ToolServer] 连接已关闭: {} - {}", code, reason);
                    connected = false;
                }
            });

            return true;

        } catch (Exception e) {
            log.error("[MCP-ToolServer] 创建连接异常: {}", e.getMessage());
            connectionLatch.countDown();
            return false;
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
     * 处理收到的消息
     */
    private void handleMessage(String text) {
        try {
            JSONObject request = JSON.parseObject(text);
            String method = request.getString("method");
            Object id = request.get("id");
            JSONObject params = request.getJSONObject("params");

            JSONObject response;

            switch (method) {
                case "initialize":
                    response = handleInitialize(id, params);
                    break;
                case "tools/list":
                    response = handleToolsList(id, params);
                    break;
                case "tools/call":
                    response = handleToolsCall(id, params);
                    break;
                case "notifications/initialized":
                    log.info("[MCP-ToolServer] 客户端已初始化");
                    return;
                default:
                    response = createErrorResponse(id, -32601, "Method not found: " + method);
            }

            if (response != null && webSocket != null) {
                String responseText = response.toJSONString();
                log.debug("[MCP-ToolServer] 发送响应: {}", responseText);
                webSocket.send(responseText);
            }

        } catch (Exception e) {
            log.error("[MCP-ToolServer] 处理消息异常: {}", e.getMessage());
        }
    }

    private JSONObject handleInitialize(Object id, JSONObject params) {
        JSONObject result = new JSONObject();
        result.put("protocolVersion", "2024-11-05");

        JSONObject capabilities = new JSONObject();
        capabilities.put("tools", new JSONObject());
        result.put("capabilities", capabilities);

        JSONObject serverInfo = new JSONObject();
        serverInfo.put("name", "English Word App MCP Tool Server");
        serverInfo.put("version", "1.0.0");
        result.put("serverInfo", serverInfo);

        return createSuccessResponse(id, result);
    }

    private JSONObject handleToolsList(Object id, JSONObject params) {
        JSONObject result = new JSONObject();
        result.put("tools", toolRegistry.getToolsArray());
        return createSuccessResponse(id, result);
    }

    private JSONObject handleToolsCall(Object id, JSONObject params) {
        String toolName = params.getString("name");
        JSONObject arguments = params.getJSONObject("arguments");
        if (arguments == null) {
            arguments = new JSONObject();
        }

        log.info("[MCP-ToolServer] 调用工具: {} 参数: {}", toolName, arguments);

        McpToolRegistry.InvokeResult invokeResult = toolRegistry.invoke(toolName, arguments);

        JSONObject content = new JSONObject();
        content.put("type", "text");

        JSONArray contentArray = new JSONArray();
        JSONObject responseResult = new JSONObject();

        if (invokeResult.isSuccess()) {
            content.put("text", invokeResult.getContent());
            contentArray.add(content);
            responseResult.put("content", contentArray);
            responseResult.put("isError", false);
        } else {
            content.put("text", "Error: " + invokeResult.getError());
            contentArray.add(content);
            responseResult.put("content", contentArray);
            responseResult.put("isError", true);
        }

        return createSuccessResponse(id, responseResult);
    }

    private JSONObject createSuccessResponse(Object id, Object result) {
        JSONObject response = new JSONObject();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("result", result);
        return response;
    }

    private JSONObject createErrorResponse(Object id, int code, String message) {
        JSONObject response = new JSONObject();
        response.put("jsonrpc", "2.0");
        response.put("id", id);

        JSONObject error = new JSONObject();
        error.put("code", code);
        error.put("message", message);
        response.put("error", error);

        return response;
    }

    @PreDestroy
    public void close() {
        if (webSocket != null) {
            webSocket.close(1000, "Tool server shutting down");
        }
        httpClient.dispatcher().executorService().shutdown();
        log.info("[MCP-ToolServer] 已关闭");
    }

    public boolean isConnected() {
        return connected;
    }
}
