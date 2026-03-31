package com.englishword.test;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.*;

import java.io.IOException;

public class ZhipuAIDemo {

    // 👉 换成你的配置
    private static final String apiKey = "686aaa8dabec4d57a159f1eaf1082512.CpkL4TBjSusy6F7N";
    private static final String apiUrl = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
    private static final String model = "glm-4-flash";

    private static final OkHttpClient httpClient = new OkHttpClient();

    public static void main(String[] args) {
        String systemPrompt = "你是一个英语学习助手";
        String userMessage = "帮我解释一下单词 'apple'";
        String conversationHistory = ""; // 可以为空

        String result = callZhipuAI(systemPrompt, userMessage, conversationHistory);

        System.out.println("AI回复：");
        System.out.println(result);
    }

    private static String callZhipuAI(String systemPrompt, String userMessage, String conversationHistory) {
        try {
            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);

            // 构建消息列表
            JSONArray messages = new JSONArray();

            // system
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);

            // 历史对话
            if (conversationHistory != null && !conversationHistory.isEmpty()) {
                JSONArray history = JSON.parseArray(conversationHistory);
                messages.addAll(history);
            }

            // user
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            requestBody.put("messages", messages);

            // 参数
            requestBody.put("temperature", 0.7);
            requestBody.put("top_p", 0.9);
            requestBody.put("max_tokens", 2000);

            // HTTP请求
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
                    System.out.println("请求失败: " + response.code());
                    return "AI服务调用失败";
                }

                String responseBody = response.body().string();
                System.out.println("原始返回：");
                System.out.println(responseBody);

                JSONObject jsonResponse = JSON.parseObject(responseBody);

                JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices != null && choices.size() > 0) {
                    JSONObject firstChoice = choices.getJSONObject(0);
                    JSONObject messageObj = firstChoice.getJSONObject("message");
                    return messageObj.getString("content");
                }

                return "解析失败";

            }

        } catch (Exception e) {
            e.printStackTrace();
            return "异常：" + e.getMessage();
        }
    }
}
