package com.englishword.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 句子实体类
 *
 * 对应数据库表：sentence
 */
@Data
@Entity
@Table(name = "sentence")
public class Sentence {

    /**
     * 句子唯一ID (UUID)
     */
    @Id
    @Column(name = "id", length = 255)
    private String id;

    /**
     * 所属用户ID
     */
    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    /**
     * 英文句子原文
     */
    @Column(name = "english_text", nullable = false, columnDefinition = "TEXT")
    private String englishText;

    /**
     * 中文翻译
     */
    @Column(name = "chinese_text", columnDefinition = "TEXT")
    private String chineseText;

    /**
     * 标记的单词（JSON数组）
     * 格式：[{"word":"apple","wordId":null},{"word":"brave","wordId":"uuid-xxx"}]
     */
    @Column(name = "marked_words", columnDefinition = "JSON")
    private String markedWords;

    /**
     * 来源对话ID（可选）
     */
    @Column(name = "source_conversation_id", length = 255)
    private String sourceConversationId;

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
        // 如果id为空，生成UUID
        if (id == null || id.isEmpty()) {
            id = java.util.UUID.randomUUID().toString();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
