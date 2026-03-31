package com.englishword.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.englishword.config.ChatProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI 兼容 API 客户端
 *
 * 支持 OpenAI 兼容格式的 API：
 * - 智谱 AI (glm-4-flash)
 * - 阿里通义千问 (qwen-plus)
 * - DeepSeek (deepseek-chat)
 * - OpenAI (gpt-4, gpt-3.5-turbo)
 */
@Slf4j
@Component
public class OpenAICompatibleClient implements ChatClient {

    private final OkHttpClient httpClient;
    private final ChatProperties config;

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

    public OpenAICompatibleClient(ChatProperties config) {
        this.config = config;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(config.getTimeout(), TimeUnit.SECONDS)
                .writeTimeout(config.getTimeout(), TimeUnit.SECONDS)
                .readTimeout(config.getTimeout(), TimeUnit.SECONDS)
                .build();

        log.info("ChatClient initialized: url={}, model={}, timeout={}s",
                maskApiKey(config.getApiUrl()), config.getModel(), config.getTimeout());
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
            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", config.getModel());

            // 构建消息列表
            JSONArray messages = new JSONArray();

            // 添加系统提示词
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.add(systemMsg);

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
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            requestBody.put("messages", messages);

            // 设置参数
            requestBody.put("temperature", config.getTemperature());
            requestBody.put("max_tokens", config.getMaxTokens());

            // 创建 HTTP 请求
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

            // 发送请求
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    log.error("AI API call failed: status={}, body={}", response.code(), errorBody);
                    return "抱歉，AI服务暂时不可用，请稍后再试。(Error: " + response.code() + ")";
                }

                String responseBody = response.body().string();
                JSONObject jsonResponse = JSON.parseObject(responseBody);

                // 提取 AI 回复
                JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    JSONObject firstChoice = choices.getJSONObject(0);
                    JSONObject messageObj = firstChoice.getJSONObject("message");
                    return messageObj.getString("content");
                }

                log.error("AI response parse failed: no choices in response");
                return "抱歉，AI回复解析失败。";
            }

        } catch (Exception e) {
            log.error("AI API call exception", e);
            return "抱歉，AI服务出现错误：" + e.getMessage();
        }
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
