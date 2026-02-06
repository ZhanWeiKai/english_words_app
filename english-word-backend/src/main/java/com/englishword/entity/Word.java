package com.englishword.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 单词实体类
 *
 * 对应数据库表：word
 */
@Data
@Entity
@Table(name = "word")
public class Word {

    /**
     * 单词唯一ID (UUID)
     */
    @Id
    @Column(name = "word_id", length = 255)
    private String wordId;

    /**
     * 所属用户ID
     */
    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    /**
     * 单词（小写）
     */
    @Column(nullable = false, length = 100)
    private String word;

    /**
     * 音标（IPA）
     */
    @Column(length = 200)
    private String pronunciation;

    /**
     * 词性（n./v./adj./adv.等）
     */
    @Column(name = "part_of_speech", length = 50)
    private String partOfSpeech;

    /**
     * 中文释义
     */
    @Column(columnDefinition = "TEXT")
    private String definition;

    /**
     * 例句（英文）
     */
    @Column(name = "example_sentence", columnDefinition = "TEXT")
    private String exampleSentence;

    /**
     * 例句翻译
     */
    @Column(name = "example_translation", columnDefinition = "TEXT")
    private String exampleTranslation;

    /**
     * 掌握程度（1-5星）
     */
    @Column(name = "mastery_level", columnDefinition = "INT DEFAULT 1")
    private Integer masteryLevel = 1;

    /**
     * 状态：LEARNING=学习中，MASTERED=已掌握
     */
    @Column(length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'LEARNING'")
    private String status = "LEARNING";

    /**
     * 添加时间
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
        // 如果wordId为空，生成UUID
        if (wordId == null || wordId.isEmpty()) {
            wordId = java.util.UUID.randomUUID().toString();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 枚举：单词状态
     */
    public enum WordStatus {
        LEARNING("LEARNING", "学习中"),
        MASTERED("MASTERED", "已掌握");

        private final String code;
        private final String description;

        WordStatus(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }
}
