package com.englishword.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI对话记录实体类
 *
 * 对应数据库表：ai_conversation
 */
@Data
@Entity
@Table(name = "ai_conversation")
public class AIConversation {

    /**
     * 对话唯一ID (UUID)
     */
    @Id
    @Column(name = "conversation_id", length = 255)
    private String conversationId;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    /**
     * 对话历史（JSON数组）
     * 格式：[{"role":"user","content":"..."},{"role":"assistant","content":"..."}]
     */
    @Column(nullable = false, columnDefinition = "JSON")
    private String messages;

    /**
     * 关联的单词ID（Word Inquiry模式）
     */
    @Column(name = "context_word_id", length = 255)
    private String contextWordId;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        // 如果conversationId为空，生成UUID
        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = java.util.UUID.randomUUID().toString();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
