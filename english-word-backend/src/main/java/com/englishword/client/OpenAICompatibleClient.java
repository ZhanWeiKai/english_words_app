package com.englishword.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.englishword.config.ChatProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI 兼容 API 客户端
 *
 * 支持 OpenAI 兼容格式的 API：
 * - 智谱 AI (glm-4-flash)
 * - 阿里通义千问 (qwen-plus)
 * - DeepSeek (deepseek-chat)
 * - OpenAI (gpt-4, gpt-3.5-turbo)
 *
 * 支持 MCP 工具调用：
 * - 自动连接 MCP Endpoint Server
 * - 获取工具列表并转换为 function calling 格式
 * - AI 自动判断是否需要调用工具
 */
@Slf4j
@Component
@Order(3)  // 在 McpToolServerRunner(1) 和 McpClient(2) 之后执行
public class OpenAICompatibleClient implements ChatClient, ApplicationRunner {

    private final OkHttpClient httpClient;
    private final ChatProperties config;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    // MCP 相关
    private final McpClient mcpClient;
    private JSONArray mcpFunctions;  // MCP 工具列表（智谱 AI function calling 格式）
    private volatile boolean mcpReady = false;  // MCP 是否就绪

    // ==================== Prompt 模板 ====================

    private static final String WORD_INQUIRY_PROMPT = """
        你是"English Word App"的专业英语AI助手，专门帮助学习者理解单词含义和用法。

        ## 你的职责
        1. **单词解释**：提供准确的中文释义、音标、词性
        2. **例句展示**：给出实用的英文例句和中文翻译
        3. **用法说明**：解释单词的使用场景和搭配
        4. **同义词对比**：提供近义词及其细微差别

        ## 回复格式要求
        - **必须使用Markdown格式**
        - 单词使用 **加粗** 标题
        - 音标使用 `代码格式`
        - 释义使用无序列表
        - 例句使用引用块 >
        - 重要提示使用加粗

        ## 示例格式
        **{word}** /{pronunciation}/ ({part_of_speech}) {中文释义}

        ### 详细释义
        - 定义1
        - 定义2

        ### 例句
        > {example_sentence}
        > 翻译：{example_translation}

        ### 用法说明
        - 使用场景1
        - 常见搭配：{collocations}

        ### 同义词
        - {synonym1}: {difference}
        - {synonym2}: {difference}

        **重要提示**：
        - 回复要友好、鼓励、专业
        - 适合中高级英语学习者
        - 例句要贴近生活场景
        """;

    private static final String SCENARIO_TRAINING_PROMPT_TEMPLATE = """
        你是一位专业的雅思口语考官，正在帮助用户练习使用目标单词。

        ## 本轮训练考词
        %s

        ## 【重要】你必须严格遵守以下输出格式

        每次提问时，你必须按以下格式输出，缺一不可：

        ```
        👨‍🏫 Examiner: [英文问题]
        中文：[中文翻译]

        （必须使用考词：[单词] /[音标]/ ＝ [中文含义]
        常见搭配：[搭配1] / [搭配2]）

        你来回答。
        ```

        ## 格式说明（必须遵守）
        1. 👨‍🏫 Examiner: 后面必须是完整的英文问题
        2. 中文：后面必须是该问题的完整中文翻译
        3. 括号内必须包含：单词、音标、中文含义、常见搭配
        4. 每个问题只针对一个考词
        5. 不评分、不总结

        ## 【关键】对话连续性规则

        用户回答后，你的下一个问题必须：
        1. **围绕用户回答的话题展开**，而不是跳到全新话题
        2. **继续深入讨论**用户提到的内容
        3. **使用下一个考词**来提问

        ### 示例：如何延续话题

        **第一轮：**
        ```
        👨‍🏫 Examiner: What products do you think have become ubiquitous in modern society?
        中文：你认为哪些产品在现代社会已经无处不在？

        （必须使用考词：ubiquitous /juːˈbɪkwɪtəs/ ＝ 无处不在的
        常见搭配：ubiquitous technology / ubiquitous presence）

        你来回答。
        ```

        **用户回答：** "I think smartphones have become ubiquitous. Almost everyone has one now."

        **你的反馈和下一轮（正确示范）：**
        ```
        ✅ 纠正：无明显错误
        📝 自然改写：Smartphones have become ubiquitous in our daily lives, with nearly everyone owning one.
        💡 提升点：可以加入更多细节，比如在哪些场景下特别明显

        ---

        👨‍🏫 Examiner: That's a great observation about smartphones! Since smartphones are everywhere now, how do you think companies should allocate their marketing budgets between mobile apps and traditional advertising?
        中文：关于智能手机的观察很到位！既然智能手机现在无处不在，你认为公司应该如何在移动应用和传统广告之间分配营销预算？

        （必须使用考词：allocate /ˈæləkeɪt/ ＝ 分配
        常见搭配：allocate resources / allocate budget）

        你来回答。
        ```

        **❌ 错误示范（不要这样）：**
        不要问与智能手机无关的问题，比如：
        "What do you think about environmental protection?"（话题跳跃太大）

        ## 用户回答后的反馈格式

        ```
        ✅ 纠正：{如有语法或用词错误，指出并纠正，没有则说"无明显错误"}
        📝 自然改写：{用更地道的方式重写用户的回答}
        💡 提升点：{可以改进的地方}

        ---

        👨‍🏫 Examiner: {基于用户回答的话题，用下一个考词提出深入问题}
        中文：[中文翻译]

        （必须使用考词：[下一个单词] /[音标]/ ＝ [中文含义]
        常见搭配：[搭配1] / [搭配2]）

        你来回答。
        ```

        ## 话题延续技巧
        - 如果用户提到"智能手机"，下一轮可以问：智能手机的影响、应用、制造商、使用习惯等
        - 如果用户提到"环境问题"，下一轮可以问：解决方案、政府责任、个人行动等
        - 始终围绕用户的话题深入，同时自然地融入考词
        - 问题之间要有逻辑连贯性，像真实的口语考试对话

        ## 注意事项
        - 每个问题必须有中文翻译
        - 每个问题必须有考词提示（音标、中文、搭配）
        - 问题要有实际意义，贴近雅思口语话题
        - 鼓励用户说完整的句子
        - **必须延续话题，不要跳跃**

        请开始第一轮训练！用第一个考词提问。
        """;

    // ==================== 构造函数 ====================

    @Autowired
    public OpenAICompatibleClient(ChatProperties config, @Autowired(required = false) McpClient mcpClient) {
        this.config = config;
        this.mcpClient = mcpClient;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(config.getTimeout(), TimeUnit.SECONDS)
                .writeTimeout(config.getTimeout(), TimeUnit.SECONDS)
                .readTimeout(config.getTimeout(), TimeUnit.SECONDS)
                .build();

        log.info("ChatClient initialized: url={}, model={}, timeout={}s, mcpEnabled={}",
                maskApiKey(config.getApiUrl()), config.getModel(), config.getTimeout(),
                mcpClient != null);
    }

    // ==================== 初始化 MCP ====================

    @Override
    public void run(ApplicationArguments args) {
        initMcp();
    }

    private void initMcp() {
        log.info("[OpenAI-Client] 初始化 MCP 工具...");
        if (mcpClient == null) {
            log.info("[OpenAI-Client] MCP 未启用，跳过工具加载");
            return;
        }

        if (!mcpClient.isConnected()) {
            log.warn("[OpenAI-Client] MCP Server 未连接，将降级为无工具模式");
            return;
        }

        try {
            log.info("[MCP] 正在获取工具列表...");
            JSONObject toolsResponse = mcpClient.listTools()
                    .get(config.getMcp().getConnectTimeout(), TimeUnit.SECONDS);

            if (toolsResponse.containsKey("error")) {
                JSONObject error = toolsResponse.getJSONObject("error");
                log.warn("[MCP] 获取工具失败: {}", error != null ? error.getString("message") : "未知错误");
                return;
            }

            JSONObject result = toolsResponse.getJSONObject("result");
            if (result == null || !result.containsKey("tools")) {
                log.warn("[MCP] 工具列表格式错误");
                return;
            }

            JSONArray mcpTools = result.getJSONArray("tools");
            this.mcpFunctions = convertMcpToolsToFunctions(mcpTools);
            this.mcpReady = true;

            log.info("[MCP] 成功加载 {} 个工具", mcpFunctions.size());
            for (int i = 0; i < mcpFunctions.size(); i++) {
                JSONObject tool = mcpFunctions.getJSONObject(i).getJSONObject("function");
                log.info("[MCP]   - {}: {}", tool.getString("name"), tool.getString("description"));
            }

        } catch (Exception e) {
            log.warn("[MCP] 初始化失败: {}，将降级为无工具模式", e.getMessage());
        }
    }

    // ==================== ChatClient 接口实现 ====================

    @Override
    public String chat(String systemPrompt, String userMessage, String conversationHistory) {
        String prompt = systemPrompt != null ? systemPrompt : "你是English Word App的英语学习助手。";
        return callApi(prompt, userMessage, conversationHistory);
    }

    @Override
    public String explainWord(String word, String conversationHistory) {
        String userMessage = String.format("请详细讲解单词：%s", word);
        return callApi(WORD_INQUIRY_PROMPT, userMessage, conversationHistory);
    }

    @Override
    public String practiceInScenario(
            List<String> trainingWords,
            String scenario,
            String userMessage,
            String conversationHistory) {

        String wordsListStr = trainingWords != null && !trainingWords.isEmpty()
                ? String.join(" / ", trainingWords)
                : "未指定";

        String systemPrompt = String.format(SCENARIO_TRAINING_PROMPT_TEMPLATE, wordsListStr);

        log.debug("practiceInScenario - words: {}, userMessage: {}",
                trainingWords, userMessage);

        return callApi(systemPrompt, userMessage, conversationHistory);
    }

    // ==================== 核心方法：调用 API ====================

    private String callApi(String systemPrompt, String userMessage, String conversationHistory) {
        try {
            // 如果 MCP 就绪，使用支持工具调用的方法
            if (mcpReady && mcpFunctions != null && !mcpFunctions.isEmpty()) {
                log.info("callApiWithTools...");
                return callApiWithTools(systemPrompt, userMessage, conversationHistory);
            } else {
                log.info("callApiSimple...");
                return callApiSimple(systemPrompt, userMessage, conversationHistory);
            }
        } catch (Exception e) {
            log.error("AI API call exception", e);
            return "抱歉，AI服务出现错误：" + e.getMessage();
        }
    }

    // ==================== 流式调用方法 ====================

    /**
     * 流式调用 AI API（支持 MCP 工具调用）
     */
    public void chatStream(String systemPrompt, String userMessage,
                           String conversationHistory, StreamCallback callback) {
        try {
            // 目前流式模式不支持工具调用，直接使用简单流式
            chatStreamSimple(systemPrompt, userMessage, conversationHistory, callback);
        } catch (Exception e) {
            log.error("AI API stream call exception", e);
            callback.onError("AI服务出现错误：" + e.getMessage());
        }
    }

    /**
     * 简单流式调用（无工具）
     * 直接使用原始响应流处理 SSE，避免 OkHttp EventSource 的兼容性问题
     */
    private void chatStreamSimple(String systemPrompt, String userMessage,
                                   String conversationHistory, StreamCallback callback) {
        // 在新线程中执行，避免阻塞
        executorService.submit(() -> {
            try {
                JSONObject requestBody = buildRequestBody(systemPrompt, userMessage, conversationHistory, null);
                requestBody.put("stream", true);  // 开启流式

                Request request = new Request.Builder()
                        .url(config.getApiUrl())
                        .addHeader("Authorization", "Bearer " + config.getApiKey())
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Accept", "text/event-stream")
                        .addHeader("Cache-Control", "no-cache")
                        .post(RequestBody.create(
                                requestBody.toJSONString(),
                                MediaType.parse("application/json; charset=utf-8")
                        ))
                        .build();

                StringBuilder fullResponse = new StringBuilder();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "No error body";
                        log.error("SSE request failed: HTTP {} - {}", response.code(), errorBody);
                        callback.onError("HTTP " + response.code() + ": " + errorBody);
                        return;
                    }

                    if (response.body() == null) {
                        callback.onError("Empty response body");
                        return;
                    }

                    // 直接读取响应流，手动解析 SSE 格式
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))) {

                        String line;
                        while ((line = reader.readLine()) != null) {
                            log.debug("[SSE] Raw line: {}", line);

                            if (line.isEmpty()) {
                                continue;  // 跳过空行
                            }

                            if (line.startsWith("data:")) {
                                String data = line.substring(5).trim();

                                if ("[DONE]".equals(data)) {
                                    log.info("[SSE] Stream completed, total length: {}", fullResponse.length());
                                    callback.onComplete(fullResponse.toString());
                                    return;
                                }

                                String chunk = parseStreamChunk(data);
                                if (chunk != null && !chunk.isEmpty()) {
                                    fullResponse.append(chunk);
                                    callback.onChunk(chunk);
                                }
                            }
                        }

                        // 如果流正常结束但没有收到 [DONE]
                        if (fullResponse.length() > 0) {
                            log.info("[SSE] Stream ended without [DONE], total length: {}", fullResponse.length());
                            callback.onComplete(fullResponse.toString());
                        }
                    }
                }

            } catch (Exception e) {
                log.error("Failed to process SSE stream", e);
                callback.onError(e.getMessage());
            }
        });
    }

    /**
     * 解析流式响应块
     * 智谱 AI 格式: {"choices":[{"delta":{"content":"xxx"}}]}
     */
    private String parseStreamChunk(String data) {
        try {
            JSONObject json = JSON.parseObject(data);
            JSONArray choices = json.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject firstChoice = choices.getJSONObject(0);
                JSONObject delta = firstChoice.getJSONObject("delta");
                if (delta != null && delta.containsKey("content")) {
                    String content = delta.getString("content");
                    if (content != null && !content.isEmpty()) {
                        return content;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse stream chunk, raw data: {}", data);
        }
        return null;
    }

    /**
     * 简单调用（无工具）
     */
    private String callApiSimple(String systemPrompt, String userMessage, String conversationHistory) throws Exception {
        JSONObject requestBody = buildRequestBody(systemPrompt, userMessage, conversationHistory, null);
        JSONObject response = sendRequest(requestBody);
        return extractContent(response);
    }

    /**
     * 支持 MCP 工具调用的方法
     */
    private String callApiWithTools(String systemPrompt, String userMessage, String conversationHistory) throws Exception {
        int maxIterations = 5;  // 最多 5 轮工具调用
        JSONArray conversationMessages = buildMessages(systemPrompt, userMessage, conversationHistory);

        for (int i = 0; i < maxIterations; i++) {
            // 构建请求
            JSONObject requestBody = buildRequestBody(null, null, null, conversationMessages);
            requestBody.put("tools", mcpFunctions);

            // 发送请求
            JSONObject response = sendRequest(requestBody);

            // 解析响应
            JSONObject message = response.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message");

            // 检查是否有工具调用
            JSONArray toolCalls = message.getJSONArray("tool_calls");

            if (toolCalls != null && !toolCalls.isEmpty()) {
                log.info("[MCP] AI 决定调用 {} 个工具", toolCalls.size());

                // 添加 assistant 消息到历史
                conversationMessages.add(message);

                // 执行每个工具调用
                for (int j = 0; j < toolCalls.size(); j++) {
                    JSONObject toolCall = toolCalls.getJSONObject(j);
                    String toolCallId = toolCall.getString("id");
                    JSONObject function = toolCall.getJSONObject("function");
                    String functionName = function.getString("name");
                    String argumentsStr = function.getString("arguments");

                    log.info("[MCP] 调用工具: {} 参数: {}", functionName, argumentsStr);

                    // 调用 MCP 工具
                    JSONObject arguments = JSON.parseObject(argumentsStr);
                    JSONObject toolResult = mcpClient.callTool(functionName, arguments)
                            .get(config.getTimeout(), TimeUnit.SECONDS);

                    String toolOutput = extractToolContent(toolResult);
                    log.info("[MCP] 工具返回: {}", toolOutput.length() > 200 ? toolOutput.substring(0, 200) + "..." : toolOutput);

                    // 添加工具结果到历史
                    JSONObject toolMessage = new JSONObject();
                    toolMessage.put("role", "tool");
                    toolMessage.put("tool_call_id", toolCallId);
                    toolMessage.put("content", toolOutput);
                    conversationMessages.add(toolMessage);
                }

                // 继续下一轮，让 AI 基于工具结果生成回复
                continue;
            }

            // 没有工具调用，返回最终内容
            String content = message.getString("content");
            return content;
        }

        return "工具调用次数超过限制";
    }

    /**
     * 构建消息列表
     */
    private JSONArray buildMessages(String systemPrompt, String userMessage, String conversationHistory) {
        JSONArray messages = new JSONArray();

        // 添加系统提示词
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.add(systemMsg);
        }

        // 添加历史对话
        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            try {
                JSONArray history = JSON.parseArray(conversationHistory);
                messages.addAll(history);
            } catch (Exception e) {
                log.warn("Failed to parse conversation history: {}", e.getMessage());
            }
        }

        // 添加当前用户消息
        if (userMessage != null && !userMessage.isEmpty()) {
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);
        }

        return messages;
    }

    /**
     * 构建请求体
     */
    private JSONObject buildRequestBody(String systemPrompt, String userMessage, String conversationHistory, JSONArray existingMessages) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", config.getModel());

        JSONArray messages;
        if (existingMessages != null) {
            messages = existingMessages;
        } else {
            messages = buildMessages(systemPrompt, userMessage, conversationHistory);
        }

        requestBody.put("messages", messages);
        requestBody.put("temperature", config.getTemperature());
        requestBody.put("max_tokens", config.getMaxTokens());

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
                .url(config.getApiUrl())
                .addHeader("Authorization", "Bearer " + config.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                throw new RuntimeException("AI API 请求失败: " + response.code() + " - " + errorBody);
            }

            String responseBody = response.body().string();
            return JSON.parseObject(responseBody);
        }
    }

    /**
     * 提取 AI 回复内容
     */
    private String extractContent(JSONObject response) {
        JSONArray choices = response.getJSONArray("choices");
        if (choices != null && !choices.isEmpty()) {
            JSONObject firstChoice = choices.getJSONObject(0);
            JSONObject messageObj = firstChoice.getJSONObject("message");
            return messageObj.getString("content");
        }
        return "AI 回复解析失败";
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

    /**
     * 将 MCP 工具格式转换为智谱 AI function calling 格式
     */
    private JSONArray convertMcpToolsToFunctions(JSONArray mcpTools) {
        JSONArray functions = new JSONArray();

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

            functions.add(tool);
        }

        return functions;
    }

    // ==================== 工具方法 ====================

    /**
     * 遮蔽 API Key 用于日志输出
     */
    private String maskApiKey(String url) {
        if (url == null) return "null";
        return url.replaceAll("(https?://[^/]+).*", "$1/**");
    }
}
