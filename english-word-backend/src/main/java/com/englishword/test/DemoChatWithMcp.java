package com.englishword.test;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.*;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * AI Chat + MCP 工具调用 Demo
 *
 * 整合智谱 AI 和 MCP 工具调用:
 * 1. 连接到 MCP Endpoint Server 获取可用工具
 * 2. 将 MCP 工具转换为智谱 AI 的 function calling 格式
 * 3. 用户提问时，AI 自动判断是否需要调用工具
 * 4. 如果需要调用工具，执行 MCP 工具并将结果返回给 AI
 * 5. AI 基于工具结果生成最终回复
 *
 * 运行顺序:
 * 1. 启动 MCP Endpoint Server (python main.py)
 * 2. 启动 DemoMcpToolServer (工具端)
 * 3. 运行本 Demo
 */
public class DemoChatWithMcp {

    // ==================== 配置 ====================

    // 智谱 AI 配置
    private static final String ZHIPU_API_KEY = "686aaa8dabec4d57a159f1eaf1082512.CpkL4TBjSusy6F7N";
    private static final String ZHIPU_API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
    private static final String ZHIPU_MODEL = "glm-4-flash";

    // MCP Endpoint Server 配置
    private static final String MCP_SERVER_URL = "ws://192.168.1.68:8004/mcp_endpoint/call/";
    private static final String MCP_TOKEN = "mvPd7HCTRs1CB7Gc/6M4AcrmtRVnQjEhKvDWbn8Nq6w=";

    // ==================== 主程序 ====================

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  AI Chat + MCP 工具调用 Demo");
        System.out.println("========================================\n");

        // 1. 连接 MCP 客户端
        McpClient mcpClient = new McpClient(MCP_SERVER_URL, MCP_TOKEN);

        System.out.println("[1] 正在连接 MCP Endpoint Server...");
        mcpClient.connect();

        if (!mcpClient.awaitConnection(5)) {
            System.err.println("连接 MCP Server 失败!");
            return;
        }
        System.out.println("[1] MCP 连接成功!\n");

        try {
            // 2. 获取 MCP 工具列表
            System.out.println("[2] 获取可用工具列表...");
            JSONObject toolsResponse = mcpClient.listTools()
                    .get(10, TimeUnit.SECONDS);

            System.out.println("[DEBUG] 工具列表响应: " + toolsResponse.toJSONString());

            if (toolsResponse.containsKey("error")) {
                JSONObject error = toolsResponse.getJSONObject("error");
                System.err.println("获取工具失败: " + (error != null ? error.getString("message") : "未知错误"));
                System.err.println("请确保 DemoMcpToolServer 已启动!");
                return;
            }

            // 3. 解析工具并转换为智谱 AI 格式
            JSONObject result = toolsResponse.getJSONObject("result");
            if (result == null || !result.containsKey("tools")) {
                System.err.println("工具列表格式错误!");
                return;
            }
            JSONArray mcpTools = result.getJSONArray("tools");

            System.out.println("[2] 发现 " + mcpTools.size() + " 个工具:");
            for (int i = 0; i < mcpTools.size(); i++) {
                JSONObject tool = mcpTools.getJSONObject(i);
                System.out.println("    - " + tool.getString("name") + ": " + tool.getString("description"));
            }
            System.out.println();

            // 4. 转换为智谱 AI function calling 格式
            JSONArray functions = convertMcpToolsToFunctions(mcpTools);

            // 5. 启动聊天循环
            System.out.println("========================================");
            System.out.println("  开始聊天 (输入 'quit' 退出)");
            System.out.println("========================================\n");

            ChatWithMcp chatClient = new ChatWithMcp(ZHIPU_API_KEY, ZHIPU_API_URL, ZHIPU_MODEL, mcpClient, functions);

            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.print("你: ");
                String userInput = scanner.nextLine().trim();

                if ("quit".equalsIgnoreCase(userInput)) {
                    break;
                }

                if (userInput.isEmpty()) {
                    continue;
                }

                System.out.println();
                String response = chatClient.chat(userInput);
                System.out.println("\nAI: " + response + "\n");
            }

            scanner.close();

        } catch (Exception e) {
            System.err.println("Demo 执行出错: " + e.getMessage());
            e.printStackTrace();
        } finally {
            mcpClient.close();
        }

        System.out.println("\n========== Demo 结束 ==========");
    }

    /**
     * 将 MCP 工具格式转换为智谱 AI function calling 格式
     * 智谱 AI tools 格式:
     * [{"type": "function", "function": {"name": "...", "description": "...", "parameters": {...}}}]
     */
    private static JSONArray convertMcpToolsToFunctions(JSONArray mcpTools) {
        JSONArray tools = new JSONArray();

        for (int i = 0; i < mcpTools.size(); i++) {
            JSONObject mcpTool = mcpTools.getJSONObject(i);

            JSONObject function = new JSONObject();
            function.put("name", mcpTool.getString("name"));
            function.put("description", mcpTool.getString("description"));

            // 转换 inputSchema -> parameters
            JSONObject inputSchema = mcpTool.getJSONObject("inputSchema");
            if (inputSchema != null) {
                function.put("parameters", inputSchema);
            }

            // 包装为智谱 AI 的 tool 格式
            JSONObject tool = new JSONObject();
            tool.put("type", "function");
            tool.put("function", function);

            tools.add(tool);
        }

        return tools;
    }

    // ==================== Chat 客户端 (支持 MCP 工具调用) ====================

    /**
     * 支持 MCP 工具调用的 Chat 客户端
     */
    static class ChatWithMcp {
        private final String apiKey;
        private final String apiUrl;
        private final String model;
        private final OkHttpClient httpClient;
        private final McpClient mcpClient;
        private final JSONArray functions;

        // 对话历史
        private JSONArray conversationHistory = new JSONArray();

        public ChatWithMcp(String apiKey, String apiUrl, String model, McpClient mcpClient, JSONArray functions) {
            this.apiKey = apiKey;
            this.apiUrl = apiUrl;
            this.model = model;
            this.mcpClient = mcpClient;
            this.functions = functions;
            this.httpClient = new OkHttpClient();
        }

        /**
         * 发送消息并获取回复 (支持工具调用)
         */
        public String chat(String userInput) {
            try {
                // 添加用户消息到历史
                JSONObject userMessage = new JSONObject();
                userMessage.put("role", "user");
                userMessage.put("content", userInput);
                conversationHistory.add(userMessage);

                // 调用 AI (可能需要多轮)
                return callAIWithTools();

            } catch (Exception e) {
                return "AI 调用出错: " + e.getMessage();
            }
        }

        /**
         * 调用 AI (支持工具调用循环)
         */
        private String callAIWithTools() throws Exception {
            int maxIterations = 5; // 最多 5 轮工具调用

            for (int i = 0; i < maxIterations; i++) {
                // 构建请求
                JSONObject requestBody = buildRequest();

                // 发送请求
                JSONObject response = sendRequest(requestBody);

                // 解析响应
                JSONObject message = response.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message");

                // 检查是否有工具调用
                JSONArray toolCalls = message.getJSONArray("tool_calls");

                if (toolCalls != null && !toolCalls.isEmpty()) {
                    // 需要调用工具
                    System.out.println("    [AI 决定调用工具...]");

                    // 添加 assistant 消息到历史
                    JSONObject assistantMessage = new JSONObject();
                    assistantMessage.put("role", "assistant");
                    assistantMessage.put("content", message.getString("content"));
                    assistantMessage.put("tool_calls", toolCalls);
                    conversationHistory.add(assistantMessage);

                    // 执行每个工具调用
                    for (int j = 0; j < toolCalls.size(); j++) {
                        JSONObject toolCall = toolCalls.getJSONObject(j);
                        String toolCallId = toolCall.getString("id");
                        JSONObject function = toolCall.getJSONObject("function");
                        String functionName = function.getString("name");
                        String argumentsStr = function.getString("arguments");

                        System.out.println("    调用工具: " + functionName);
                        System.out.println("    参数: " + argumentsStr);

                        // 调用 MCP 工具
                        JSONObject arguments = JSON.parseObject(argumentsStr);
                        JSONObject toolResult = mcpClient.callTool(functionName, arguments)
                                .get(30, TimeUnit.SECONDS);

                        // 提取工具返回内容
                        String toolOutput = extractToolContent(toolResult);
                        System.out.println("    工具返回: " + toolOutput);

                        // 添加工具结果到历史
                        JSONObject toolMessage = new JSONObject();
                        toolMessage.put("role", "tool");
                        toolMessage.put("tool_call_id", toolCallId);
                        toolMessage.put("content", toolOutput);
                        conversationHistory.add(toolMessage);
                    }

                    // 继续下一轮，让 AI 基于工具结果生成回复
                    continue;
                }

                // 没有工具调用，返回最终结果
                String content = message.getString("content");

                // 添加到历史
                JSONObject assistantMessage = new JSONObject();
                assistantMessage.put("role", "assistant");
                assistantMessage.put("content", content);
                conversationHistory.add(assistantMessage);

                return content;
            }

            return "工具调用次数超过限制";
        }

        /**
         * 构建请求体
         */
        private JSONObject buildRequest() {
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);
            requestBody.put("messages", conversationHistory);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);

            // 添加工具定义 (智谱 AI 格式)
            if (functions != null && !functions.isEmpty()) {
                requestBody.put("tools", functions);
            }

            return requestBody;
        }

        /**
         * 发送 HTTP 请求
         */
        private JSONObject sendRequest(JSONObject requestBody) throws Exception {
            RequestBody body = RequestBody.create(
                    requestBody.toJSONString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("请求失败: " + response.code());
                }

                String responseBody = response.body().string();
                return JSON.parseObject(responseBody);
            }
        }

        /**
         * 从 MCP 工具响应中提取内容
         */
        private String extractToolContent(JSONObject toolResult) {
            if (toolResult.containsKey("result")) {
                JSONObject result = toolResult.getJSONObject("result");
                JSONArray content = result.getJSONArray("content");
                if (content != null && !content.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < content.size(); i++) {
                        JSONObject item = content.getJSONObject(i);
                        if ("text".equals(item.getString("type"))) {
                            sb.append(item.getString("text"));
                        }
                    }
                    return sb.toString();
                }
            }

            if (toolResult.containsKey("error")) {
                return "Error: " + toolResult.getJSONObject("error").getString("message");
            }

            return toolResult.toJSONString();
        }
    }
}
