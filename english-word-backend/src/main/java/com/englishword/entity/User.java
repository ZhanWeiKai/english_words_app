package com.englishword.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体类
 *
 * 对应数据库表：user
 */
@Data
@Entity
@Table(name = "user")
public class User {

    /**
     * 用户唯一ID (UUID)
     */
    @Id
    @Column(name = "user_id", length = 255)
    private String userId;

    /**
     * 用户名（登录用）
     */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * 密码（BCrypt加密）
     */
    @Column(nullable = false)
    private String password;

    /**
     * 昵称
     */
    @Column(length = 100)
    private String nickname;

    /**
     * 头像URL
     */
    @Column(length = 500)
    private String avatar;

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
        // 如果userId为空，生成UUID
        if (userId == null || userId.isEmpty()) {
            userId = java.util.UUID.randomUUID().toString();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
