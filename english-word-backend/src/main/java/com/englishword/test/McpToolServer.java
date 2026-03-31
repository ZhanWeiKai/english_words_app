package com.englishword.test;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.*;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * MCP 工具端服务器
 *
 * 连接到 MCP Endpoint Server 并注册工具，处理工具调用请求
 *
 * 运行方式:
 * 1. 先启动 MCP Endpoint Server (python main.py)
 * 2. 运行此类的 main 方法
 * 3. 然后运行 DemoMcpClient 来调用工具
 */
public class McpToolServer {

    private final String serverUrl;
    private final String token;
    private WebSocket webSocket;
    private final OkHttpClient httpClient;
    private final CountDownLatch connectionLatch = new CountDownLatch(1);
    private volatile boolean connected = false;

    // 注册的工具: name -> ToolInfo
    private final Map<String, ToolInfo> tools = new HashMap<>();

    /**
     * 工具处理器接口
     */
    @FunctionalInterface
    public interface ToolHandler {
        Object handle(JSONObject arguments);
    }

    /**
     * 工具信息类
     */
    public static class ToolInfo {
        final String name;
        final String description;
        final JSONObject inputSchema;
        final ToolHandler handler;

        public ToolInfo(String name, String description, JSONObject inputSchema, ToolHandler handler) {
            this.name = name;
            this.description = description;
            this.inputSchema = inputSchema;
            this.handler = handler;
        }
    }

    public McpToolServer(String serverUrl, String token) {
        this.serverUrl = serverUrl;
        this.token = token;
        this.httpClient = new OkHttpClient.Builder()
                .pingInterval(java.time.Duration.ofSeconds(30))
                .build();
    }

    /**
     * 注册工具
     * @param name 工具名称
     * @param description 工具描述
     * @param inputSchema 输入参数 schema (JSON Schema 格式)
     * @param handler 工具处理函数
     */
    public void registerTool(String name, String description, JSONObject inputSchema, ToolHandler handler) {
        ToolInfo tool = new ToolInfo(name, description, inputSchema, handler);
        tools.put(name, tool);
        System.out.println("[MCP-Tool] 已注册工具: " + name);
    }

    /**
     * 连接到 MCP Endpoint Server
     */
    public void connect() {
        try {
            String encodedToken = URLEncoder.encode(token, "UTF-8");
            String wsUrl = serverUrl + "?token=" + encodedToken;

            System.out.println("[MCP-Tool] 正在连接: " + wsUrl);

            Request request = new Request.Builder()
                    .url(wsUrl)
                    .build();

            webSocket = httpClient.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    System.out.println("[MCP-Tool] 已连接到 MCP Endpoint Server");
                    connected = true;
                    connectionLatch.countDown();
                }

                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    System.out.println("[MCP-Tool] 收到请求: " + text);
                    handleMessage(text);
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    System.err.println("[MCP-Tool] 连接失败: " + t.getMessage());
                    connected = false;
                    connectionLatch.countDown();
                }

                @Override
                public void onClosing(WebSocket webSocket, int code, String reason) {
                    System.out.println("[MCP-Tool] 连接正在关闭: " + code + " - " + reason);
                }

                @Override
                public void onClosed(WebSocket webSocket, int code, String reason) {
                    System.out.println("[MCP-Tool] 连接已关闭: " + code + " - " + reason);
                    connected = false;
                }
            });
        } catch (Exception e) {
            System.err.println("[MCP-Tool] 连接异常: " + e.getMessage());
            connectionLatch.countDown();
        }
    }

    /**
     * 等待连接建立
     */
    public boolean awaitConnection(long timeoutSeconds) {
        try {
            return connectionLatch.await(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS) && connected;
        } catch (InterruptedException e) {
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
                    // 这是一个通知，不需要响应
                    System.out.println("[MCP-Tool] 客户端已初始化");
                    return;
                default:
                    response = createErrorResponse(id, -32601, "Method not found: " + method);
            }

            if (response != null) {
                String responseText = response.toJSONString();
                System.out.println("[MCP-Tool] 发送响应: " + responseText);
                webSocket.send(responseText);
            }

        } catch (Exception e) {
            System.err.println("[MCP-Tool] 处理消息异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 处理 initialize 请求
     */
    private JSONObject handleInitialize(Object id, JSONObject params) {
        JSONObject result = new JSONObject();
        result.put("protocolVersion", "2024-11-05");

        JSONObject capabilities = new JSONObject();
        capabilities.put("tools", new JSONObject());
        result.put("capabilities", capabilities);

        JSONObject serverInfo = new JSONObject();
        serverInfo.put("name", "Java MCP Tool Server");
        serverInfo.put("version", "1.0.0");
        result.put("serverInfo", serverInfo);

        return createSuccessResponse(id, result);
    }

    /**
     * 处理 tools/list 请求
     */
    private JSONObject handleToolsList(Object id, JSONObject params) {
        JSONObject result = new JSONObject();
        JSONArray toolsArray = new JSONArray();

        for (ToolInfo tool : tools.values()) {
            JSONObject toolDef = new JSONObject();
            toolDef.put("name", tool.name);
            toolDef.put("description", tool.description);
            toolDef.put("inputSchema", tool.inputSchema);
            toolsArray.add(toolDef);
        }

        result.put("tools", toolsArray);
        return createSuccessResponse(id, result);
    }

    /**
     * 处理 tools/call 请求
     */
    private JSONObject handleToolsCall(Object id, JSONObject params) {
        String toolName = params.getString("name");
        JSONObject arguments = params.getJSONObject("arguments");

        if (arguments == null) {
            arguments = new JSONObject();
        }

        ToolInfo tool = tools.get(toolName);
        if (tool == null) {
            return createErrorResponse(id, -32602, "Unknown tool: " + toolName);
        }

        try {
            Object result = tool.handler.handle(arguments);

            JSONObject content = new JSONObject();
            content.put("type", "text");
            content.put("text", String.valueOf(result));

            JSONArray contentArray = new JSONArray();
            contentArray.add(content);

            JSONObject responseResult = new JSONObject();
            responseResult.put("content", contentArray);
            responseResult.put("isError", false);

            return createSuccessResponse(id, responseResult);

        } catch (Exception e) {
            JSONObject content = new JSONObject();
            content.put("type", "text");
            content.put("text", "Error: " + e.getMessage());

            JSONArray contentArray = new JSONArray();
            contentArray.add(content);

            JSONObject responseResult = new JSONObject();
            responseResult.put("content", contentArray);
            responseResult.put("isError", true);

            return createSuccessResponse(id, responseResult);
        }
    }

    /**
     * 创建成功响应
     */
    private JSONObject createSuccessResponse(Object id, Object result) {
        JSONObject response = new JSONObject();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("result", result);
        return response;
    }

    /**
     * 创建错误响应
     */
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

    /**
     * 关闭连接
     */
    public void close() {
        if (webSocket != null) {
            webSocket.close(1000, "Tool server shutting down");
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
