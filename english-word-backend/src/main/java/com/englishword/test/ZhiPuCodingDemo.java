package com.englishword.test;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.model.*;
import java.util.Arrays;


public class ZhiPuCodingDemo {
    public static void main(String[] args) {
        // 初始化客户端
        ZhipuAiClient client = ZhipuAiClient.builder().ofZHIPU()
                .apiKey("686aaa8dabec4d57a159f1eaf1082512.CpkL4TBjSusy6F7N")
                .build();

        // 创建聊天完成请求
        ChatCompletionCreateParams request = ChatCompletionCreateParams.builder()
                .model("glm-5")
                .messages(Arrays.asList(
                        ChatMessage.builder()
                                .role(ChatMessageRole.USER.value())
                                .content("Hello, who are you?")
                                .build()
                ))
                .stream(false)
                .temperature(0.6f)
                .maxTokens(1024)
                .build();

        // 发送请求
        ChatCompletionResponse response = client.chat().createChatCompletion(request);

        // 获取回复
        System.out.println(response.getData().getChoices().get(0).getMessage());
    }
}
