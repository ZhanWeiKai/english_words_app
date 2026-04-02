# Sentence Management Feature Design

## Overview

Add a sentence management feature that allows users to save useful English sentences with Chinese translations and marked important words. Users interact through AI chat to create/edit/delete sentences, and view them in a paginated list with search.

## Database Design

### New Table: `sentence`

Migration file: `V2__add_sentence_table.sql`

```sql
CREATE TABLE sentence (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    english_text TEXT NOT NULL COMMENT '英文句子原文',
    chinese_text TEXT COMMENT '中文翻译',
    marked_words JSON COMMENT '标记的单词，如 [{"word":"apple","wordId":null},{"word":"brave","wordId":"uuid-xxx"}]',
    source_conversation_id VARCHAR(255) COMMENT '来源对话ID（可选）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_sentence_user_id (user_id),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### `marked_words` JSON Structure

```json
[
  {"word": "apple", "wordId": null},
  {"word": "brave", "wordId": "550e8400-e29b-41d4-a716-446655440000"}
]
```

- `word`: the word text (always present)
- `wordId`: null if not in word vault, UUID if linked to a word in the vault

### Dynamic Linking

When a user adds a word to their word vault, the backend scans all their sentences and updates any matching `marked_words` entries from `wordId: null` to the new word's ID. This is handled by a dedicated API endpoint.

## API Design

All endpoints follow existing project patterns: JWT auth, `ApiResponse<T>` wrapper, Swagger docs.

### 1. List Sentences (Paginated + Search)

```
GET /api/sentences?page=0&size=20&keyword=apple
```

- `keyword` optional, searches english_text, chinese_text, and marked_words (fuzzy match)
- Returns paginated results ordered by created_at DESC

### 2. Get Sentence Detail

```
GET /api/sentences/{id}
```

### 3. Create Sentence

```
POST /api/sentences
Body: {
  "englishText": "The apple is very delicious",
  "chineseText": "这个苹果非常美味",
  "markedWords": [
    {"word": "apple", "wordId": null},
    {"word": "delicious", "wordId": "uuid-xxx"}
  ],
  "sourceConversationId": "conv-uuid"
}
```

### 4. Update Sentence

```
PUT /api/sentences/{id}
Body: {
  "englishText": "...",
  "chineseText": "...",
  "markedWords": [...]
}
```

### 5. Delete Sentence

```
DELETE /api/sentences/{id}
```

### 6. Link Word to Sentences

```
POST /api/sentences/link-word
Body: {
  "word": "apple",
  "wordId": "uuid-xxx"
}
```

Scans all sentences of the current user, updates `markedWords` entries where `word` matches and `wordId` is null.

## Architecture

### New Files

- **Entity**: `Sentence.java` - JPA entity with UUID generation, auto timestamps
- **Repository**: `SentenceRepository.java` - JPA repository with custom search query
- **Service**: `SentenceService.java` - CRUD + search + link-word logic
- **Controller**: `SentenceController.java` - REST endpoints with Swagger docs
- **DTO**: `SentenceRequest.java`, `MarkedWordDTO.java` - request/response objects
- **Migration**: `V2__add_sentence_table.sql` - Flyway migration

### Design Decisions

1. **JSON field for marked_words** over join table - consistent with existing project patterns (ai_conversation.messages, training_session.word_ids already use JSON), simpler for small per-user data volumes
2. **AI chat driven** - users manage sentences through AI conversation, not a traditional CRUD form
3. **Dynamic linking** - word associations upgrade from plain text to vault link when words are added later
