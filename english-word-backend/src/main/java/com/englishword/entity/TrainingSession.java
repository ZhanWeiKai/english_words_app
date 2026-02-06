package com.englishword.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 训练会话实体类
 *
 * 对应数据库表：training_session
 */
@Data
@Entity
@Table(name = "training_session")
public class TrainingSession {

    /**
     * 会话唯一ID (UUID)
     */
    @Id
    @Column(name = "session_id", length = 255)
    private String sessionId;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    /**
     * 训练的单词ID列表（JSON格式）
     */
    @Column(name = "word_ids", nullable = false, columnDefinition = "JSON")
    private String wordIds;

    /**
     * 开始时间
     */
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @Column(name = "end_time")
    private LocalDateTime endTime;

    /**
     * 训练结果（JSON格式）
     * 包含单词ID、掌握等级变化等
     */
    @Column(columnDefinition = "JSON")
    private String results;

    @PrePersist
    protected void onCreate() {
        startTime = LocalDateTime.now();
        // 如果sessionId为空，生成UUID
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = java.util.UUID.randomUUID().toString();
        }
    }
}
