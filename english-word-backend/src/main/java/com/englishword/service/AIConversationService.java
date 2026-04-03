package com.englishword.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONArray;
import com.englishword.client.ChatClient;
import com.englishword.context.UserContext;
import com.englishword.dto.request.AIChatRequest;
import com.englishword.dto.response.ApiResponse;
import com.englishword.dto.response.AIChatResponse;
import com.englishword.dto.response.WordResult;
import com.englishword.entity.AIConversation;
import com.englishword.repository.AIConversationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.*;

/**
 * AI对话服务
 *
 * 功能：
 * - 管理AI对话记录
 * - 调用 ChatClient 获取 AI 回复
 * - 生成建议操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIConversationService {

    private final ChatClient chatClient;
    private final AIConversationRepository conversationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 处理AI聊天请求
     *
     * @param userId 用户ID
     * @param request 聊天请求
     * @return AI回复
     */
    @Transactional
    public ApiResponse<AIChatResponse> chat(String userId, AIChatRequest request) {
        try {
            String conversationId = request.getConversationId();
            String conversationHistory;

            // 1. 获取或创建对话记录
            AIConversation conversation;
            if (conversationId != null && !conversationId.isEmpty()) {
                // 继续历史对话
                Optional<AIConversation> convOptional = conversationRepository.findById(conversationId);
                if (convOptional.isEmpty()) {
                    return ApiResponse.error(404, "对话不存在");
                }
                conversation = convOptional.get();

                // 验证权限
                if (!conversation.getUserId().equals(userId)) {
                    return ApiResponse.error(403, "无权访问此对话");
                }

                conversationHistory = conversation.getMessages();
            } else {
                // 创建新对话
                conversation = new AIConversation();
                conversation.setUserId(userId);
                conversation.setContextWordId(request.getTargetWord());
                conversation.setMessages("[]"); // 初始化空对话历史
                conversation = conversationRepository.save(conversation);
                conversationId = conversation.getConversationId();
                conversationHistory = "[]";
            }

            // 2. 设置当前操作用户（供 MCP 工具跨线程使用）
            UserContext.setCurrentOperationUser(userId, null);
            log.info("[AIConversationService] 设置操作用户: userId={}", userId);

            // 3. 根据模式调用AI
            String aiReply;
            String mode = request.getMode();
            log.info("=== AI Chat Request === Mode: {}, TrainingWords: {}, TargetWord: {}",
                    mode, request.getTrainingWords(), request.getTargetWord());

            try {
                if ("word_training".equals(mode)) {
                    // 训练模式
                    aiReply = chatClient.practiceInScenario(
                            request.getTrainingWords(),
                            request.getScenario(),
                            request.getMessage(),
                            conversationHistory
                    );
                } else {
                    log.info("chatClient.chat 通用模式");
                    // 默认：通用对话
                    aiReply = chatClient.chat(
                            null,  // 使用默认系统提示词
                            request.getMessage(),
                            conversationHistory
                    );
                }
            } finally {
                // 4. 清除操作用户（无论成功失败都要清除）
                UserContext.clearOperationUser();
            }

            // 5. 更新对话历史
            List<Map<String, String>> messages = parseMessages(conversationHistory);
            messages.add(Map.of("role", "user", "content", request.getMessage()));
            messages.add(Map.of("role", "assistant", "content", aiReply));

            // 限制历史记录长度（最近20轮）
            if (messages.size() > 20) {
                messages = messages.subList(messages.size() - 20, messages.size());
            }

            conversation.setMessages(objectMapper.writeValueAsString(messages));
            conversationRepository.save(conversation);

            // 4. 构建响应
            AIChatResponse response = new AIChatResponse();
            response.setConversationId(conversationId);
            response.setMessage(aiReply);

            // 生成建议操作
            List<AIChatResponse.Suggestion> suggestions = generateSuggestions(mode, request.getTargetWord());
            response.setSuggestions(suggestions);

            return ApiResponse.success(response, "AI回复成功");

        } catch (Exception e) {
            log.error("AI聊天处理失败", e);
            return ApiResponse.error(500, "AI聊天处理失败：" + e.getMessage());
        }
    }

    /**
     * 流式聊天 - 返回 Flux 用于 SSE
     *
     * @param userId  用户ID
     * @param request 聊天请求
     * @return SSE 事件流
     */
    public Flux<String> chatStream(String userId, AIChatRequest request) {
        return Flux.create(emitter -> {
            try {
                // 1. 获取或创建对话
                String conversationId = request.getConversationId();
                AIConversation conversation;
                String conversationHistory;

                if (conversationId != null && !conversationId.isEmpty()) {
                    Optional<AIConversation> convOptional = conversationRepository.findById(conversationId);
                    if (convOptional.isEmpty()) {
                        emitter.error(new RuntimeException("对话不存在"));
                        return;
                    }
                    conversation = convOptional.get();
                    if (!conversation.getUserId().equals(userId)) {
                        emitter.error(new RuntimeException("无权访问此对话"));
                        return;
                    }
                    conversationHistory = conversation.getMessages();
                } else {
                    conversation = new AIConversation();
                    conversation.setUserId(userId);
                    conversation.setContextWordId(request.getTargetWord());
                    conversation.setMessages("[]");
                    conversation = conversationRepository.save(conversation);
                    conversationId = conversation.getConversationId();
                    conversationHistory = "[]";
                }

                // 2. 设置用户上下文
                UserContext.setCurrentOperationUser(userId, null);

                // 3. 先发送 conversationId 给客户端（使用特殊前缀标识）
                emitter.next("__CONVERSATION_ID__:" + conversationId);

                // 4. 收集完整响应用于保存
                StringBuilder fullResponse = new StringBuilder();
                String finalConversationId = conversationId;
                String finalConversationHistory = conversationHistory;
                AIConversation finalConversation = conversation;

                // 5. 调用流式 API
                String systemPrompt = getSystemPrompt(request.getMode(), request.getTrainingWords());

                chatClient.chatStream(
                        systemPrompt,
                        request.getMessage(),
                        finalConversationHistory,
                        new ChatClient.StreamCallback() {
                            @Override
                            public void onChunk(String chunk) {
                                fullResponse.append(chunk);
                                // 发送 SSE 格式的数据
                                emitter.next(chunk);
                            }

                            @Override
                            public void onComplete(String response) {
                                try {
                                    // 6. 保存对话历史
                                    saveConversationHistory(finalConversation, request.getMessage(), response);

                                    // 7. 发送完成标记
                                    emitter.next("[DONE]");
                                    emitter.complete();
                                } finally {
                                    UserContext.clearOperationUser();
                                }
                            }

                            @Override
                            public void onError(String error) {
                                UserContext.clearOperationUser();
                                emitter.error(new RuntimeException(error));
                            }

                            @Override
                            public void onToolCall(String toolName, String arguments) {
                                // 发送工具调用事件给前端
                                emitter.next("__TOOL_CALL__:" + toolName);
                            }
                        }
                );

            } catch (Exception e) {
                log.error("Stream chat error", e);
                UserContext.clearOperationUser();
                emitter.error(e);
            }
        }, FluxSink.OverflowStrategy.BUFFER);
    }

    /**
     * 根据模式获取系统提示词
     */
    private String getSystemPrompt(String mode, List<String> trainingWords) {
        if ("word_training".equals(mode) && trainingWords != null && !trainingWords.isEmpty()) {
            String wordsListStr = String.join(" / ", trainingWords);
            return String.format(SCENARIO_TRAINING_PROMPT_TEMPLATE, wordsListStr);
        }
        return null;  // 使用默认提示词
    }

    private static final String SCENARIO_TRAINING_PROMPT_TEMPLATE = """
        你是一位专业的雅思口语考官，正在帮助用户练习使用目标单词。

        ## 本轮训练考词
        %s

        请用这些单词与用户进行对话练习。
        """;

    /**
     * 保存对话历史
     */
    private void saveConversationHistory(AIConversation conversation, String userMessage, String aiReply) {
        try {
            String conversationHistory = conversation.getMessages();
            List<Map<String, String>> messages = parseMessages(conversationHistory);
            messages.add(Map.of("role", "user", "content", userMessage));
            messages.add(Map.of("role", "assistant", "content", aiReply));

            // 限制历史记录长度（最近20轮）
            if (messages.size() > 20) {
                messages = messages.subList(messages.size() - 20, messages.size());
            }

            conversation.setMessages(objectMapper.writeValueAsString(messages));
            conversationRepository.save(conversation);
            log.info("Saved conversation history for conversationId={}", conversation.getConversationId());
        } catch (Exception e) {
            log.error("Failed to save conversation history", e);
        }
    }

    /**
     * 获取对话历史
     *
     * @param userId 用户ID
     * @param conversationId 对话ID
     * @return 对话历史
     */
    public ApiResponse<AIConversation> getConversation(String userId, String conversationId) {
        Optional<AIConversation> convOptional = conversationRepository.findById(conversationId);

        if (convOptional.isEmpty()) {
            return ApiResponse.error(404, "对话不存在");
        }

        AIConversation conversation = convOptional.get();

        // 验证权限
        if (!conversation.getUserId().equals(userId)) {
            return ApiResponse.error(403, "无权访问此对话");
        }

        return ApiResponse.success(conversation);
    }

    /**
     * 获取用户的对话列表
     *
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 对话列表
     */
    public ApiResponse<List<AIConversation>> getUserConversations(String userId, int page, int size) {
        List<AIConversation> conversations = conversationRepository
                .findTop10ByUserIdOrderByCreatedAtDesc(userId);

        return ApiResponse.success(conversations);
    }

    /**
     * 解析消息历史
     */
    private List<Map<String, String>> parseMessages(String messagesJson) {
        try {
            if (messagesJson == null || messagesJson.isEmpty() || "[]".equals(messagesJson)) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(messagesJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
        } catch (Exception e) {
            log.error("解析消息历史失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 生成建议操作
     */
    private List<AIChatResponse.Suggestion> generateSuggestions(String mode, String targetWord) {
        List<AIChatResponse.Suggestion> suggestions = new ArrayList<>();

        if ("word_inquiry".equals(mode) && targetWord != null) {
            // 询问模式：建议添加到单词本和开始训练
            AIChatResponse.Suggestion suggestion1 = new AIChatResponse.Suggestion();
            suggestion1.setType("add_to_list");
            suggestion1.setWord(targetWord);
            suggestion1.setLabel("添加到单词本");
            suggestions.add(suggestion1);

            AIChatResponse.Suggestion suggestion2 = new AIChatResponse.Suggestion();
            suggestion2.setType("train");
            suggestion2.setWord(targetWord);
            suggestion2.setLabel("开始训练");
            suggestions.add(suggestion2);
        }

        return suggestions;
    }

    /**
     * 解析AI返回的单词搜索结果
     */
    private List<WordResult> parseWordResults(String aiReply) {
        try {
            // 提取JSON部分（可能被markdown代码块包裹）
            String jsonContent = aiReply;

            // 如果包含```json，提取其中的内容
            if (aiReply.contains("```json")) {
                int start = aiReply.indexOf("```json") + 7;
                int end = aiReply.indexOf("```", start);
                if (end > start) {
                    jsonContent = aiReply.substring(start, end).trim();
                }
            } else if (aiReply.contains("```")) {
                int start = aiReply.indexOf("```") + 3;
                int end = aiReply.indexOf("```", start);
                if (end > start) {
                    jsonContent = aiReply.substring(start, end).trim();
                }
            }

            // 解析JSON
            JSONObject jsonResponse = JSON.parseObject(jsonContent);
            JSONArray wordsArray = jsonResponse.getJSONArray("words");

            if (wordsArray == null || wordsArray.isEmpty()) {
                return null;
            }

            List<WordResult> results = new ArrayList<>();
            for (int i = 0; i < wordsArray.size(); i++) {
                JSONObject wordObj = wordsArray.getJSONObject(i);
                WordResult result = new WordResult();
                result.setWord(wordObj.getString("word"));
                result.setPhonetic(wordObj.getString("phonetic"));
                result.setPartOfSpeech(wordObj.getString("partOfSpeech"));
                result.setMeaning(wordObj.getString("meaning"));
                result.setExample(wordObj.getString("example"));
                results.add(result);
            }

            return results;

        } catch (Exception e) {
            log.error("Failed to parse word results JSON: {}", e.getMessage());
            return null;
        }
    }
}
