package com.englishword.client;

import java.util.List;

/**
 * AI Chat 客户端接口
 *
 * 定义 AI 聊天的核心方法，支持多种实现（OpenAI 兼容 API、自定义等）
 */
public interface ChatClient {

    /**
     * 流式回调接口
     */
    interface StreamCallback {
        /**
         * 收到一个文本块
         */
        void onChunk(String chunk);

        /**
         * 流式完成，返回完整响应
         */
        void onComplete(String fullResponse);

        /**
         * 发生错误
         */
        void onError(String error);

        /**
         * 工具调用通知（可选实现）
         * 当 AI 决定调用 MCP 工具时触发，前端可显示 "正在调用 xxx..."
         *
         * @param toolName 工具名称
         * @param arguments 工具参数（JSON 字符串）
         */
        default void onToolCall(String toolName, String arguments) {
            // 默认空实现，保持向后兼容
        }
    }

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
     * 通用对话（流式输出）
     *
     * @param systemPrompt 系统提示词
     * @param userMessage 用户消息
     * @param conversationHistory 对话历史（JSON 数组字符串，可为 null）
     * @param callback 流式回调
     */
    void chatStream(String systemPrompt, String userMessage, String conversationHistory, StreamCallback callback);

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
