package com.englishword.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * AI聊天响应
 */
@Data
@Schema(description = "AI聊天响应")
public class AIChatResponse {

    @Schema(description = "对话ID")
    private String conversationId;

    @Schema(description = "AI回复消息（Markdown格式）")
    private String message;

    @Schema(description = "建议操作列表")
    private List<Suggestion> suggestions;

    @Data
    @Schema(description = "建议操作")
    public static class Suggestion {
        @Schema(description = "操作类型：add_to_list 或 train")
        private String type;

        @Schema(description = "单词")
        private String word;

        @Schema(description = "显示文本")
        private String label;
    }
}
