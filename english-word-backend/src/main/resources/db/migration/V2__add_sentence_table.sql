-- =====================================================
-- V2: Add sentence table for sentence management feature
-- =====================================================

-- -----------------------------------------------------
-- Sentence table
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS sentence (
    id VARCHAR(255) NOT NULL PRIMARY KEY COMMENT 'Sentence unique ID (UUID)',
    user_id VARCHAR(255) NOT NULL COMMENT 'User ID',
    english_text TEXT NOT NULL COMMENT 'English sentence text',
    chinese_text TEXT COMMENT 'Chinese translation',
    marked_words JSON COMMENT 'Marked words, e.g. [{"word":"apple","wordId":null},{"word":"brave","wordId":"uuid-xxx"}]',
    source_conversation_id VARCHAR(255) COMMENT 'Source conversation ID (optional)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    INDEX idx_sentence_user_id (user_id),
    INDEX idx_sentence_created_at (created_at),
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Sentence table';
