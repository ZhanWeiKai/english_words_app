-- English Word App 数据库初始化脚本
-- 创建数据库：CREATE DATABASE english_word_app CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
    nickname VARCHAR(50) COMMENT '昵称',
    avatar VARCHAR(255) COMMENT '头像',
    status INT NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-正常',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 单词表
CREATE TABLE IF NOT EXISTS words (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '单词ID',
    word VARCHAR(100) NOT NULL UNIQUE COMMENT '单词',
    phonetic VARCHAR(50) COMMENT '音标',
    part_of_speech VARCHAR(20) COMMENT '词性',
    definition TEXT COMMENT '释义',
    example TEXT COMMENT '例句',
    synonyms TEXT COMMENT '同义词',
    antonyms TEXT COMMENT '反义词',
    word_root TEXT COMMENT '词根词缀',
    memory_tip TEXT COMMENT '记忆技巧',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_word (word),
    FULLTEXT INDEX ft_word_definition (word, definition)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='单词表';

-- 学习记录表
CREATE TABLE IF NOT EXISTS learning_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    word_id BIGINT NOT NULL COMMENT '单词ID',
    status INT NOT NULL COMMENT '状态 0-未学习 1-学习中 2-已掌握',
    review_count INT DEFAULT 0 COMMENT '复习次数',
    correct_count INT DEFAULT 0 COMMENT '正确次数',
    next_review_date DATETIME COMMENT '下次复习时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_word (user_id, word_id),
    INDEX idx_user_id (user_id),
    INDEX idx_next_review (next_review_date),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (word_id) REFERENCES words(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习记录表';

-- 插入示例数据
INSERT INTO words (word, phonetic, part_of_speech, definition, example, synonyms, antonyms, word_root, memory_tip) VALUES
('abandon', '/əˈbændən/', 'v.', '放弃；遗弃；抛弃', 'The baby''s mother had abandoned him.', 'give up, quit', 'keep, maintain', 'a-(不) + ban-(禁止) + -don(给予)', '联想：一个禁止令被放弃了'),
('ability', '/əˈbɪləti/', 'n.', '能力；才能', 'She has the ability to pass the exam.', 'capability, talent', 'inability', 'able + -ity(名词后缀)', 'able的名词形式，表示"能力"'),
('about', '/əˈbaʊt/', 'prep./adv.', '关于；大约', 'Tell me about yourself.', 'regarding, approximately', NULL, 'a- + bout', 'a(一个) + bout( boutle瓶子) = 围绕一个瓶子'),
('above', '/əˈbʌv/', 'prep./adv.', '在...之上；超过', 'The plane flew above the clouds.', 'over, exceeding', 'below, under', 'a- + bove(类似bove)", "联想：在头顶上方"');
