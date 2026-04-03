package com.englishword.controller;

import com.englishword.dto.request.AIChatRequest;
import com.englishword.dto.response.ApiResponse;
import com.englishword.dto.response.AIChatResponse;
import com.englishword.entity.AIConversation;
import com.englishword.service.AIConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI聊天控制器
 *
 * 提供的API：
 * - POST /api/ai/chat - AI对话（HTTP，非流式）
 * - POST /api/ai/chat/stream - AI对话（SSE流式）
 * - GET /api/ai/conversations/{id} - 获取对话历史
 * - GET /api/ai/conversations - 获取对话列表
 */
@Tag(name = "AI聊天", description = "AI智能助手接口")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIConversationService aiConversationService;

    /**
     * AI对话（HTTP，非流式）
     *
     * @param request 聊天请求
     * @param httpRequest HTTP请求
     * @return AI回复
     */
    @Operation(summary = "AI对话（非流式）", description = "与AI助手进行对话，等待完整响应后返回")
    @PostMapping("/chat")
    public ApiResponse<AIChatResponse> chat(
            @RequestBody AIChatRequest request,
            HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return aiConversationService.chat(userId, request);
    }

    /**
     * AI对话（SSE流式）
     *
     * 流式返回AI回复，打字机效果。
     * 第一个事件是 conversationId，后续是文本块，最后是 [DONE]。
     *
     * @param request 聊天请求
     * @param httpRequest HTTP请求
     * @return SSE事件流
     */
    @Operation(summary = "AI对话（流式）", description = "与AI助手进行对话，流式返回（打字机效果）")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(
            @RequestBody AIChatRequest request,
            HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");

        return aiConversationService.chatStream(userId, request)
                .map(chunk -> {
                    // 第一个事件以 "__CONVERSATION_ID__:" 开头的是 conversationId
                    if (chunk.startsWith("__CONVERSATION_ID__:")) {
                        return ServerSentEvent.<String>builder()
                                .event("conversationId")
                                .data(chunk.substring(20))  // 去掉前缀
                                .build();
                    }
                    // 工具调用事件
                    if (chunk.startsWith("__TOOL_CALL__:")) {
                        return ServerSentEvent.<String>builder()
                                .event("tool_call")
                                .data(chunk.substring(14))  // 只返回工具名
                                .build();
                    }
                    // [DONE] 标记
                    if ("[DONE]".equals(chunk)) {
                        return ServerSentEvent.<String>builder()
                                .event("done")
                                .data("[DONE]")
                                .build();
                    }
                    // 普通文本块
                    return ServerSentEvent.<String>builder()
                            .event("message")
                            .data(chunk)
                            .build();
                });
    }

    /**
     * 获取对话历史
     *
     * @param conversationId 对话ID
     * @param httpRequest HTTP请求
     * @return 对话记录
     */
    @Operation(summary = "获取对话历史", description = "根据对话ID获取完整的对话记录")
    @GetMapping("/conversations/{conversationId}")
    public ApiResponse<AIConversation> getConversation(
            @Parameter(description = "对话ID") @PathVariable String conversationId,
            HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return aiConversationService.getConversation(userId, conversationId);
    }

    /**
     * 获取对话列表
     *
     * @param page 页码
     * @param size 每页大小
     * @param httpRequest HTTP请求
     * @return 对话列表
     */
    @Operation(summary = "获取对话列表", description = "获取当前用户的所有对话记录")
    @GetMapping("/conversations")
    public ApiResponse<List<AIConversation>> getConversations(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return aiConversationService.getUserConversations(userId, page, size);
    }
}
