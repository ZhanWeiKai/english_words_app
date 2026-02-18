package com.englishword.service;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 独立测试 ZhipuAI Prompt 格式
 * 不需要 Spring 容器，直接测试 AI 响应格式
 */
public class ZhipuAIPromptTest {

    private static final String API_KEY = "cea9d940b7b7498d916e1c924ba3b6ca.zwaG7aTXwBW60Dr4";
    private static final String API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
    private static final String MODEL = "glm-4-flash";

    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("测试 ZhipuAI 训练 Prompt 格式");
        System.out.println("========================================\n");

        // 构建训练单词
        List<String> trainingWords = List.of("allocate", "complex", "sustainable");
        String wordsListStr = String.join(" / ", trainingWords);

        // 构建 System Prompt（与 ZhipuAIService.java 中相同）
        String systemPrompt = String.format("""
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

            ## 示例输出

            ```
            👨‍🏫 Examiner: Cities today face many development challenges. What do you think is the biggest difficulty for modern cities?
            中文：现代城市面临很多发展挑战，你认为最大的困难是什么？

            （必须使用考词：complex /ˈkɒmpleks/ ＝ 复杂的
            常见搭配：a complex problem / a complex system）

            你来回答。
            ```

            请开始第一轮训练！用第一个考词提问。
            """, wordsListStr);

        System.out.println("【发送的 System Prompt】:");
        System.out.println("----------------------------------------");
        System.out.println(systemPrompt);
        System.out.println("----------------------------------------\n");

        // 调用 API
        String response = callZhipuAI(systemPrompt, "Let's start!");

        System.out.println("【AI 响应】:");
        System.out.println("========================================");
        System.out.println(response);
        System.out.println("========================================\n");

        // 验证格式
        System.out.println("【格式验证】:");
        boolean hasExaminer = response.contains("👨‍🏫 Examiner:");
        boolean hasChinese = response.contains("中文：");
        boolean hasWordTip = response.contains("必须使用考词：");
        boolean hasPhonetic = response.contains("/") && response.contains("＝");
        boolean hasCollocation = response.contains("常见搭配：");

        System.out.println("✅ 包含 Examiner: " + hasExaminer);
        System.out.println("✅ 包含中文翻译: " + hasChinese);
        System.out.println("✅ 包含考词提示: " + hasWordTip);
        System.out.println("✅ 包含音标: " + hasPhonetic);
        System.out.println("✅ 包含搭配: " + hasCollocation);

        boolean allPassed = hasExaminer && hasChinese && hasWordTip && hasPhonetic && hasCollocation;
        System.out.println("\n【测试结果】: " + (allPassed ? "✅ 通过" : "❌ 失败"));

        if (!allPassed) {
            System.exit(1);
        }
    }

    private static String callZhipuAI(String systemPrompt, String userMessage) throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        // 构建请求
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", MODEL);

        JSONArray messages = new JSONArray();

        JSONObject systemMsg = new JSONObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.put(systemMsg);

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.put(userMsg);

        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 1000);

        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("API 调用失败: " + response.code());
            }

            String responseBody = response.body().string();
            JSONObject jsonResponse = new JSONObject(responseBody);

            JSONArray choices = jsonResponse.getJSONArray("choices");
            if (choices != null && choices.length() > 0) {
                JSONObject firstChoice = choices.getJSONObject(0);
                JSONObject messageObj = firstChoice.getJSONObject("message");
                return messageObj.getString("content");
            }

            return "解析响应失败";
        }
    }
}
