package com.englishword.client;

import java.util.List;

/**
 * AI Chat 客户端接口
 *
 * 定义 AI 聊天的核心方法，支持多种实现（OpenAI 兼容 API、自定义等）
 */
public interface ChatClient {

    /**
     * 通用对话
     *
     * @param systemPrompt 系统提示词
     * @param userMessage 用户消息
     * @param conversationHistory 对话历史（JSON 数组字符串，可为 null）
     * @return AI 回复
     */
    String chat(String systemPrompt, String userMessage, String conversationHistory);

    /**
     * 单词讲解
     *
     * @param word 要讲解的单词
     * @param conversationHistory 对话历史（JSON 数组字符串，可为 null）
     * @return AI 回复（包含音标、释义、例句等）
     */
    String explainWord(String word, String conversationHistory);

    /**
     * 场景训练
     *
     * @param trainingWords 训练单词列表
     * @param scenario 场景描述（可为 null）
     * @param userMessage 用户消息
     * @param conversationHistory 对话历史（JSON 数组字符串，可为 null）
     * @return AI 回复（考官风格的训练对话）
     */
    String practiceInScenario(
            List<String> trainingWords,
            String scenario,
            String userMessage,
            String conversationHistory
    );
}
