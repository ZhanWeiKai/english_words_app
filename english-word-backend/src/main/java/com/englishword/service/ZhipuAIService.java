package com.englishword.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 智谱AI服务
 *
 * 功能：
 * - 调用智谱AI GLM-4-Flash API
 * - Word Inquiry模式：单词讲解
 * - Word Training模式：场景训练对话
 */
@Slf4j
@Service
public class ZhipuAIService {

    private final OkHttpClient httpClient;
    private final String apiKey;
    private final String apiUrl;
    private final String model;

    /**
     * 构造函数
     */
    public ZhipuAIService(
            @Value("${zhipuai.api-key}") String apiKey,
            @Value("${zhipuai.api-url}") String apiUrl,
            @Value("${zhipuai.model}") String model) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;

        // 配置HTTP客户端
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Word Inquiry模式：单词讲解
     *
     * @param word 单词
     * @param conversationHistory 对话历史（可选）
     * @return AI回复
     */
    public String explainWord(String word, String conversationHistory) {
        String systemPrompt = buildWordInquiryPrompt();
        String userMessage = String.format("请详细讲解单词：%s", word);

        return callZhipuAI(systemPrompt, userMessage, conversationHistory);
    }

    /**
     * Word Training模式：场景训练
     *
     * @param targetWord 目标单词
     * @param scenario 场景描述
     * @param trainingWords 训练单词列表
     * @param conversationHistory 对话历史
     * @return AI回复
     */
    public String practiceInScenario(String targetWord, String scenario, List<String> trainingWords, String conversationHistory) {
        String systemPrompt = buildScenarioTrainingPrompt(targetWord, scenario, trainingWords);
        String userMessage = "Let's start!"; // 用户准备开始

        return callZhipuAI(systemPrompt, userMessage, conversationHistory);
    }

    /**
     * 通用AI对话
     *
     * @param message 用户消息
     * @param conversationHistory 对话历史
     * @param systemPrompt 系统提示词（可选）
     * @return AI回复
     */
    public String chat(String message, String conversationHistory, String systemPrompt) {
        String prompt = systemPrompt != null ? systemPrompt : "你是English Word App的英语学习助手。";
        return callZhipuAI(prompt, message, conversationHistory);
    }

    /**
     * 调用智谱AI API
     *
     * @param systemPrompt 系统提示词
     * @param userMessage 用户消息
     * @param conversationHistory 对话历史（JSON字符串）
     * @return AI回复
     */
    private String callZhipuAI(String systemPrompt, String userMessage, String conversationHistory) {
        try {
            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);

            // 构建消息列表
            JSONArray messages = new JSONArray();

            // 添加系统提示词
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);

            // 添加历史对话（如果有）
            if (conversationHistory != null && !conversationHistory.isEmpty()) {
                JSONArray history = JSON.parseArray(conversationHistory);
                messages.addAll(history);
            }

            // 添加当前用户消息
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            requestBody.put("messages", messages);

            // 设置参数
            requestBody.put("temperature", 0.7);
            requestBody.put("top_p", 0.9);
            requestBody.put("max_tokens", 2000);

            // 创建HTTP请求
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

            // 发送请求
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("智谱AI调用失败：{}", response.code());
                    return "抱歉，AI服务暂时不可用，请稍后再试。";
                }

                String responseBody = response.body().string();
                JSONObject jsonResponse = JSON.parseObject(responseBody);

                // 提取AI回复
                JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices != null && choices.size() > 0) {
                    JSONObject firstChoice = choices.getJSONObject(0);
                    JSONObject messageObj = firstChoice.getJSONObject("message");
                    return messageObj.getString("content");
                }

                return "抱歉，AI回复解析失败。";
            }

        } catch (Exception e) {
            log.error("调用智谱AI异常", e);
            return "抱歉，AI服务出现错误：" + e.getMessage();
        }
    }

    /**
     * 构建Word Inquiry模式的系统提示词
     */
    private String buildWordInquiryPrompt() {
        return """
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
    }

    /**
     * 构建Word Training模式的系统提示词
     */
    private String buildScenarioTrainingPrompt(String targetWord, String scenario, List<String> trainingWords) {
        // 构建训练单词列表
        String wordsListStr = "";
        if (trainingWords != null && !trainingWords.isEmpty()) {
            wordsListStr = String.join(" / ", trainingWords);
        } else if (targetWord != null) {
            wordsListStr = targetWord;
        }

        return String.format("""
            你是一位专业的雅思口语考官，正在帮助用户练习使用目标单词。

            ## 本轮训练考词
            %s

            ## 训练格式要求（必须严格遵守）
            ✅ 考官问题：英文 + 中文
            ✅ 放在 writing block 里（使用```格式）
            ✅ 括号中给：考词 + 音标 + 中文 + 搭配
            ✅ 用户回答后：必要纠正 + 自然改写 + 提升点
            ❌ 不评分
            ❌ 不总结

            ## 你的职责
            1. **提出问题**：每次只问一个问题，让用户使用指定的考词回答
            2. **轮换考词**：依次使用每个考词提问，让用户练习所有单词
            3. **提供反馈**：用户回答后给出纠正和改写建议

            ## 回复格式示例

            👨‍🏫 Examiner: What do you think are the main challenges facing modern cities?
            中文：你认为现代城市面临的主要挑战是什么？

            （必须使用考词：complex /ˈkɒmpleks/ = 复杂的
            常见搭配：a complex problem / a complex system）

            你来回答。

            ---

            用户回答后，你的反馈格式：

            ✅ 纠正：{如有语法或用词错误，指出并纠正}
            📝 自然改写：{用更地道的方式重写用户的回答}
            💡 提升点：{可以改进的地方}

            然后继续下一个问题，使用下一个考词。

            ## 注意事项
            - 每个问题只针对一个考词
            - 问题要有实际意义，贴近雅思口语话题
            - 鼓励用户说完整的句子
            - 反馈要具体、有建设性

            请开始第一轮训练！
            """, wordsListStr);
    }
}
