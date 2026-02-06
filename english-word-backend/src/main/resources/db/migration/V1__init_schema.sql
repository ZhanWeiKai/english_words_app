-- =====================================================
-- English Word App 数据库初始化脚本
-- =====================================================
-- 数据库创建命令：
-- CREATE DATABASE english_word_app CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- =====================================================

-- -----------------------------------------------------
-- 1. 用户表 (user)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS user (
    user_id VARCHAR(255) PRIMARY KEY COMMENT '用户唯一ID (UUID)',
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名（登录用）',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    nickname VARCHAR(100) COMMENT '昵称',
    avatar VARCHAR(500) COMMENT '头像URL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- -----------------------------------------------------
-- 2. 单词表 (word)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS word (
    word_id VARCHAR(255) PRIMARY KEY COMMENT '单词唯一ID (UUID)',
    user_id VARCHAR(255) NOT NULL COMMENT '所属用户ID',
    word VARCHAR(100) NOT NULL COMMENT '单词（小写）',
    pronunciation VARCHAR(200) COMMENT '音标（IPA）',
    part_of_speech VARCHAR(50) COMMENT '词性（n./v./adj./adv.等）',
    definition TEXT COMMENT '中文释义',
    example_sentence TEXT COMMENT '例句（英文）',
    example_translation TEXT COMMENT '例句翻译',
    mastery_level INT DEFAULT 1 COMMENT '掌握程度（1-5星）',
    status VARCHAR(20) DEFAULT 'LEARNING' COMMENT '状态：LEARNING=学习中，MASTERED=已掌握',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_word (word),
    INDEX idx_status (status),
    INDEX idx_mastery_level (mastery_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='单词表';

-- -----------------------------------------------------
-- 3. 训练会话表 (training_session)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS training_session (
    session_id VARCHAR(255) PRIMARY KEY COMMENT '会话唯一ID (UUID)',
    user_id VARCHAR(255) NOT NULL COMMENT '用户ID',
    word_ids JSON NOT NULL COMMENT '训练的单词ID列表',
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    end_time TIMESTAMP NULL COMMENT '结束时间',
    results JSON COMMENT '训练结果（单词ID、掌握等级变化等）',
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_start_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='训练会话表';

-- -----------------------------------------------------
-- 4. AI对话记录表 (ai_conversation)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS ai_conversation (
    conversation_id VARCHAR(255) PRIMARY KEY COMMENT '对话唯一ID (UUID)',
    user_id VARCHAR(255) NOT NULL COMMENT '用户ID',
    messages JSON NOT NULL COMMENT '对话历史（JSON数组）',
    context_word_id VARCHAR(255) COMMENT '关联的单词ID（Word Inquiry模式）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话记录表';

-- -----------------------------------------------------
-- 插入测试用户（密码：123456，BCrypt加密后的值）
-- -----------------------------------------------------
INSERT INTO user (user_id, username, password, nickname) VALUES
('user_test_001', 'testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '测试用户');

-- -----------------------------------------------------
-- 插入示例单词数据
-- -----------------------------------------------------
INSERT INTO word (word_id, user_id, word, pronunciation, part_of_speech, definition, example_sentence, example_translation, mastery_level, status) VALUES
('word_001', 'user_test_001', 'ephemeral', '/ɪˈfemərəl/', 'adj.', '短暂的；瞬息的', 'Fashion is ephemeral, changing with every season.', '时尚是短暂的，每一季都在变化。', 2, 'LEARNING'),
('word_002', 'user_test_001', 'serendipity', '/ˌserənˈdɪpəti/', 'n.', '意外发现珍奇事物的运气；机缘巧合', 'Meeting her was pure serendipity.', '遇见她纯属机缘巧合。', 1, 'LEARNING'),
('word_003', 'user_test_001', 'ubiquitous', '/juːˈbɪkwɪtəs/', 'adj.', '无处不在的；普遍存在的', 'Smartphones have become ubiquitous in modern life.', '智能手机在现代生活中已经无处不在。', 3, 'LEARNING');

-- -----------------------------------------------------
-- 索引说明
-- -----------------------------------------------------
-- user表：username索引用于快速登录查询
-- word表：user_id索引用于查询用户的单词列表
-- word表：status索引用于筛选学习状态
-- word表：mastery_level索引用于按掌握程度排序
-- training_session表：user_id和start_time索引用于查询训练历史
-- ai_conversation表：user_id和created_at索引用于查询对话历史
