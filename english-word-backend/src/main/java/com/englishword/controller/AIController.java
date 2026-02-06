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
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI聊天控制器
 *
 * 提供的API：
 * - POST /api/ai/chat - AI对话（HTTP）
 * - GET /api/ai/conversations/{id} - 获取对话历史
 * - GET /api/ai/conversations - 获取对话列表
 *
 * WebSocket端点：
 * - ws://localhost:8885/api/ws - 实时AI对话
 */
@Tag(name = "AI聊天", description = "AI智能助手接口")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIConversationService aiConversationService;

    /**
     * AI对话（HTTP）
     *
     * @param request 聊天请求
     * @param httpRequest HTTP请求
     * @return AI回复
     */
    @Operation(summary = "AI对话", description = "与AI助手进行对话（HTTP方式）")
    @PostMapping("/chat")
    public ApiResponse<AIChatResponse> chat(
            @RequestBody AIChatRequest request,
            HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return aiConversationService.chat(userId, request);
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
