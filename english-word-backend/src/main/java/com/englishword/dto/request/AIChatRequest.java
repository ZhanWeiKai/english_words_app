package com.englishword.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI聊天请求
 */
@Data
@Schema(description = "AI聊天请求")
public class AIChatRequest {

    @Schema(description = "用户消息", required = true, example = "ephemeral是什么意思？")
    private String message;

    @Schema(description = "对话ID（继续历史对话时使用）", example = "conv_001")
    private String conversationId;

    @Schema(description = "模式：word_inquiry 或 word_training", example = "word_inquiry")
    private String mode;

    @Schema(description = "目标单词（training模式使用）", example = "ephemeral")
    private String targetWord;

    @Schema(description = "场景描述（training模式使用）", example = "You're at a coffee shop")
    private String scenario;
}
