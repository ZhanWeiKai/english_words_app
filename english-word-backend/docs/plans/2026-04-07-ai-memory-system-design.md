# AI Memory System 设计文档

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 构建记忆驱动的 AI 系统，AI 通过对话自动学习用户信息、创建的功能、以及对话事件，并按需检索相关记忆来个性化回复。

**Tech Stack:** Spring Boot, MySQL, 向量存储, OpenAI Embedding

---

## 1. 三种记忆类型

| 类型 | 内容 | 存储时长 | 用途 |
|-----|------|---------|------|
| **PROFILE** | 用户信息（身份、目标、能力、偏好） | 永久 | 个性化回复 |
| **CAPABILITY** | 用户创建的模式/功能 | 永久 | 复用用户定义的功能 |
| **EVENT** | 对话事件总结 | 永久 | 记住做过什么 |

---

## 2. 整体流程

```
┌─────────────────────────────────────────────────────────────┐
│                      用户发送消息                            │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│              1. 向量检索相关记忆（三种类型同时检索）            │
│                                                             │
│  PROFILE: Top 3 (相似度 >= 0.5)                              │
│  CAPABILITY: Top 2 (相似度 >= 0.5)                           │
│  EVENT: Top 3 (相似度 >= 0.5)                                │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│              2. 组合成 Context 注入 AI                       │
│                                                             │
│  [PROFILE] 用户是程序员，想考雅思7分，偏好美式英语            │
│  [CAPABILITY] word practice: 针对单词场景对话                │
│  [EVENT] 上次学习了 interview, negotiate                     │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│              3. AI 回复 + 判断是否存储记忆                    │
│                                                             │
│  • 用户说了关于自己的信息？→ 存/更新 PROFILE                  │
│  • 用户创建了新模式？→ 存/更新 CAPABILITY                    │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│              4. Conversation 结束时（10分钟无消息）            │
│                                                             │
│  • AI 总结本次对话 → 存/更新 EVENT                           │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. 存储时机

### 3.1 PROFILE - 用户信息

**何时存储：** 用户在对话中提到关于自己的信息

**触发示例：**

| 用户说 | 存储内容 |
|-------|---------|
| "我是程序员" | PROFILE: "职业：程序员" |
| "我想考雅思7分" | PROFILE: "目标：雅思7分" |
| "我喜欢美式英语" | PROFILE: "偏好：美式英语" |
| "我词汇量大概5000" | PROFILE: "词汇量：5000" |

**存储逻辑：**
```
1. AI 判断用户消息是否包含个人信息
2. 如果是，检查是否已有相同类型的 PROFILE
3. 有则更新，无则新增
```

### 3.2 CAPABILITY - 用户创建的模式

**何时存储：** 用户创建一个新的模式/功能

**触发示例：**

| 用户说 | 存储内容 |
|-------|---------|
| "帮我创建一个 word practice 模式，针对单词场景对话" | CAPABILITY: "word practice" |
| "创建一个 shadowing 模式练口语" | CAPABILITY: "shadowing" |
| "设计一个单词复习流程" | CAPABILITY: "单词复习流程" |

**存储逻辑：**
```
1. AI 判断用户是否在创建新模式/功能
2. 检查是否已有同名 CAPABILITY
3. 有则更新，无则新增
```

### 3.3 EVENT - 对话事件总结

**何时存储：** Conversation 结束时（10分钟无新消息）

**总结示例：**

| 对话内容 | EVENT 总结 |
|---------|-----------|
| 用户学单词，学了5个，掌握3个 | "word practice 学习单词: abandon, ability... 共5个，掌握3个，不熟2个(abstract, accelerate)" |
| 模拟面试，问了5道题 | "模拟Java面试: HashMap、IOC、MySQL索引、Redis、分布式事务，答对3道，不熟2道(Redis、分布式事务)" |
| 刷 LeetCode | "刷LeetCode: 做了5道题，动态规划薄弱" |

**存储逻辑：**
```
1. 检测 Conversation 是否结束（10分钟无新消息）
2. AI 总结本次对话的关键内容
3. 检查该 Conversation 是否已有 EVENT
4. 有则更新（同一 conversationId），无则新增
```

---

## 4. 检索时机与方式

### 4.1 检索时机

**每次用户发送消息时，同时检索三种类型。**

### 4.2 检索方式

```
用户消息 → 生成向量 embedding
              │
              ▼
        在三种记忆类型中分别计算相似度
              │
              ├─ PROFILE: 取相似度 >= 0.5 的 Top 3
              ├─ CAPABILITY: 取相似度 >= 0.5 的 Top 2
              └─ EVENT: 取相似度 >= 0.5 的 Top 3
              │
              ▼
        组合成 Context 注入 AI
```

### 4.3 检索示例

**用户消息：** "帮我制定一个学习计划"

**向量检索：**

```
查询向量: embed("学习计划")

PROFILE 匹配 (相似度 >= 0.5):
├─ "程序员，想考雅思7分，偏好美式英语" ✓ 0.78
└─ "词汇量5000" ✓ 0.52

CAPABILITY 匹配 (相似度 >= 0.5):
├─ "word practice: 针对单词场景对话" ✓ 0.51
└─ (无其他匹配)

EVENT 匹配 (相似度 >= 0.5):
├─ "word practice 学习 interview, negotiate" ✓ 0.62
├─ "模拟雅思口语练习" ✓ 0.58
└─ "刷LeetCode: 动态规划" ✓ 0.50
```

**组合 Context：**

```
[PROFILE]
• 程序员，想考雅思7分，偏好美式英语
• 词汇量5000

[CAPABILITY]
• word practice: 针对单词场景对话

[EVENT - 最近学习]
• word practice 学习 interview, negotiate
• 模拟雅思口语练习
• 刷LeetCode: 动态规划
```

**AI 回复：**

```
根据你的情况，制定以下计划：

📋 目标：雅思7分
👤 背景：程序员，词汇量5000，美式英语偏好

📚 建议计划：
1. 继续用 word practice 扩充词汇
   （已学：interview, negotiate）
2. 多做雅思口语模拟
3. 结合你程序员背景，可以多看技术文档

今天要从哪个开始？
```

---

## 5. 完整对话示例

### Day 1: 用户首次对话

```
用户: 我是程序员，想考雅思7分，喜欢美式英语

[检索：无相关记忆]

AI: 好的，了解了。你是程序员，目标雅思7分，偏好美式英语。

[存储 PROFILE]
PROFILE(id=1): "程序员，想考雅思7分，偏好美式英语"

───────────────────────────────────────

用户: 帮我创建一个 word practice 模式，可以针对某个单词进行场景对话

[检索]
PROFILE: "程序员，想考雅思7分..." ✓ 0.52

AI: 好的，已创建 word practice 模式。

[存储 CAPABILITY]
CAPABILITY(id=1): "word practice - 针对某个单词进行场景对话"

───────────────────────────────────────

用户: 进入 word practice，学 interview

[检索]
CAPABILITY: "word practice..." ✓ 0.92
PROFILE: "偏好美式英语" ✓ 0.54

AI: 好的，进入 word practice 模式。
    根据你喜欢美式英语，我选一个美式职场场景...
    interview 是"面试"的意思。
    场景：你明天要去一家外企面试...

[继续学习...]

───────────────────────────────────────

用户: 好了，今天到这

AI: 好的，今天学了 interview，你掌握得不错。

--- 10分钟后无新消息，Conversation 结束 ---

[存储 EVENT]
EVENT(id=1, conversation_id=101): "word practice 学习 interview，已掌握"
```

### Day 2: 用户继续学习

```
用户: 继续 word practice

[检索]
CAPABILITY: "word practice..." ✓ 0.95
EVENT: "word practice 学习 interview，已掌握" ✓ 0.72

AI: 好的，继续 word practice。
    你上次学了 interview，今天学新单词吗？

用户: 学 negotiate

AI: negotiate 是"谈判"的意思。
    场景：你在和客户谈合同价格...

[继续学习...]

--- Conversation 结束 ---

[更新 EVENT]
EVENT(id=2, conversation_id=102): "word practice 学习 negotiate，已掌握"
```

### Day 5: 用户问学习计划

```
用户: 帮我制定一个学习计划

[检索]
PROFILE: "程序员，想考雅思7分，偏好美式英语" ✓ 0.78
CAPABILITY: "word practice..." ✓ 0.51
EVENT: "word practice 学习 interview, negotiate" ✓ 0.62
        "模拟雅思口语" ✓ 0.58

AI: 根据你的情况，制定以下计划：

    📋 目标：雅思7分
    👤 背景：程序员，美式英语偏好

    📚 建议计划：
    1. 继续用 word practice 扩充词汇
       （已学：interview, negotiate）
    2. 多做雅思口语模拟

    今天要从哪个开始？
```

---

## 6. 项目实现

### 6.1 数据库表设计

```sql
CREATE TABLE user_memory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    conversation_id BIGINT,                    -- EVENT 专用，关联对话
    memory_type ENUM('PROFILE', 'CAPABILITY', 'EVENT') NOT NULL,
    content TEXT NOT NULL,                     -- 记忆内容
    embedding BLOB,                            -- 向量（1536维 float32）
    metadata JSON,                             -- 扩展字段
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_user_type (user_id, memory_type),
    INDEX idx_conversation (conversation_id)
);
```

### 6.2 核心代码结构

```
src/main/java/com/englishword/
├── entity/
│   ├── UserMemory.java                 -- 记忆实体
│   └── MemoryType.java                 -- 记忆类型枚举
├── repository/
│   └── UserMemoryRepository.java       -- 数据访问层
├── service/
│   ├── MemoryService.java              -- 记忆管理服务
│   ├── MemoryExtractionService.java    -- 记忆提取服务
│   └── VectorService.java              -- 向量计算服务
├── dto/
│   └── MemoryContext.java              -- 记忆上下文 DTO
└── job/
    └── ConversationEndDetector.java    -- 对话结束检测
```

### 6.3 核心服务实现

#### MemoryService.java

```java
@Service
@RequiredArgsConstructor
public class MemoryService {

    private final UserMemoryRepository repository;
    private final VectorService vectorService;

    /**
     * 检索相关记忆
     */
    public MemoryContext retrieveMemories(Long userId, String query) {
        float[] queryVector = vectorService.embed(query);

        List<UserMemory> profiles = repository.findTopKByTypeAndSimilarity(
            userId, MemoryType.PROFILE, queryVector, 0.5f, 3);

        List<UserMemory> capabilities = repository.findTopKByTypeAndSimilarity(
            userId, MemoryType.CAPABILITY, queryVector, 0.5f, 2);

        List<UserMemory> events = repository.findTopKByTypeAndSimilarity(
            userId, MemoryType.EVENT, queryVector, 0.5f, 3);

        return new MemoryContext(profiles, capabilities, events);
    }

    /**
     * 存储或更新记忆
     */
    public void saveMemory(Long userId, Long conversationId,
                          MemoryType type, String content) {
        float[] embedding = vectorService.embed(content);

        Optional<UserMemory> existing = findExisting(userId, type, content);

        if (existing.isPresent()) {
            // 更新
            UserMemory memory = existing.get();
            memory.setContent(content);
            memory.setEmbedding(embedding);
            repository.save(memory);
        } else {
            // 新增
            UserMemory memory = new UserMemory();
            memory.setUserId(userId);
            memory.setConversationId(conversationId);
            memory.setMemoryType(type);
            memory.setContent(content);
            memory.setEmbedding(embedding);
            repository.save(memory);
        }
    }

    /**
     * 查找已有记忆（PROFILE/CAPABILITY 按内容相似度，EVENT 按 conversationId）
     */
    private Optional<UserMemory> findExisting(Long userId, MemoryType type, String content) {
        if (type == MemoryType.EVENT) {
            return Optional.empty(); // EVENT 总是新建或按 conversationId 更新
        }
        // PROFILE/CAPABILITY: 查找相似度高的
        float[] vector = vectorService.embed(content);
        return repository.findTopByTypeAndSimilarity(userId, type, vector, 0.8f);
    }
}
```

#### MemoryExtractionService.java

```java
@Service
@RequiredArgsConstructor
public class MemoryExtractionService {

    private final ZhipuAIService aiService;
    private final MemoryService memoryService;

    /**
     * 从对话中提取记忆
     */
    public void extractAndSave(Long userId, Long conversationId,
                               String userMessage, String aiResponse) {

        String extractionPrompt = """
            分析用户消息，判断是否需要存储记忆。

            记忆类型：
            - PROFILE: 用户关于自己的信息（身份、目标、能力、偏好）
            - CAPABILITY: 用户创建的模式或功能

            用户消息: %s

            如果需要存储，返回JSON：
            {"store": true, "type": "PROFILE或CAPABILITY", "content": "提取的内容"}

            如果不需要存储，返回：
            {"store": false}
            """.formatted(userMessage);

        String result = aiService.chat(extractionPrompt);
        ExtractionResult extraction = parseResult(result);

        if (extraction.isStore()) {
            memoryService.saveMemory(userId, conversationId,
                extraction.getType(), extraction.getContent());
        }
    }

    /**
     * 对话结束时总结 EVENT
     */
    public void summarizeEvent(Long userId, Long conversationId,
                               List<ChatMessage> messages) {

        String summaryPrompt = """
            总结这次对话的关键内容，用于记忆存储。

            对话内容：
            %s

            返回一句话总结，包含：
            - 做了什么
            - 学了什么
            - 重要结果
            """.formatted(formatMessages(messages));

        String summary = aiService.chat(summaryPrompt);
        memoryService.saveMemory(userId, conversationId, MemoryType.EVENT, summary);
    }
}
```

#### ConversationEndDetector.java

```java
@Component
@RequiredArgsConstructor
public class ConversationEndDetector {

    private final MemoryExtractionService extractionService;
    private final ConversationRepository conversationRepo;

    // 每5分钟检查一次
    @Scheduled(fixedRate = 300000)
    public void detectEndedConversations() {
        // 查找10分钟无消息的对话
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);
        List<Conversation> ended = conversationRepo.findByLastMessageBeforeAndNotSummarized(threshold);

        for (Conversation conv : ended) {
            // 总结并存入 EVENT
            extractionService.summarizeEvent(
                conv.getUserId(),
                conv.getId(),
                conv.getMessages()
            );

            // 标记已总结
            conv.setSummarized(true);
            conversationRepo.save(conv);
        }
    }
}
```

### 6.4 集成到 AIConversationService

```java
@Service
@RequiredArgsConstructor
public class AIConversationService {

    private final MemoryService memoryService;
    private final MemoryExtractionService extractionService;
    private final ZhipuAIService aiService;

    public String chat(Long userId, Long conversationId, String userMessage) {

        // 1. 检索相关记忆
        MemoryContext context = memoryService.retrieveMemories(userId, userMessage);

        // 2. 构建带记忆的 prompt
        String systemPrompt = buildSystemPrompt(context);

        // 3. 调用 AI
        String aiResponse = aiService.chat(systemPrompt, userMessage);

        // 4. 提取并存储记忆
        extractionService.extractAndSave(userId, conversationId, userMessage, aiResponse);

        return aiResponse;
    }

    private String buildSystemPrompt(MemoryContext context) {
        StringBuilder sb = new StringBuilder();

        if (!context.getProfiles().isEmpty()) {
            sb.append("[用户信息]\n");
            context.getProfiles().forEach(p -> sb.append("• ").append(p.getContent()).append("\n"));
            sb.append("\n");
        }

        if (!context.getCapabilities().isEmpty()) {
            sb.append("[用户创建的模式]\n");
            context.getCapabilities().forEach(c -> sb.append("• ").append(c.getContent()).append("\n"));
            sb.append("\n");
        }

        if (!context.getEvents().isEmpty()) {
            sb.append("[用户最近经历]\n");
            context.getEvents().forEach(e -> sb.append("• ").append(e.getContent()).append("\n"));
            sb.append("\n");
        }

        sb.append("请根据以上信息个性化回复用户。");

        return sb.toString();
    }
}
```

### 6.5 向量检索实现

```java
@Service
public class VectorService {

    @Value("${ai.embedding.api-url}")
    private String apiUrl;

    @Value("${ai.embedding.api-key}")
    private String apiKey;

    /**
     * 调用 Embedding API 生成向量
     */
    public float[] embed(String text) {
        // 调用 OpenAI 或智谱 Embedding API
        // 返回 1536 维向量
    }

    /**
     * 计算余弦相似度
     */
    public float cosineSimilarity(float[] a, float[] b) {
        float dotProduct = 0;
        float normA = 0;
        float normB = 0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        return dotProduct / (float) (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
```

**Repository 向量检索：**

```java
public interface UserMemoryRepository extends JpaRepository<UserMemory, Long> {

    @Query(value = """
        SELECT * FROM user_memory
        WHERE user_id = :userId
          AND memory_type = :type
        ORDER BY VECTOR_COSINE(embedding, :queryVector) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<UserMemory> findTopKByTypeAndSimilarity(
        @Param("userId") Long userId,
        @Param("type") String type,
        @Param("queryVector") byte[] queryVector,
        @Param("threshold") float threshold,
        @Param("limit") int limit
    );
}
```

**注意：** MySQL 原生不支持向量检索，可选择：
1. **MySQL 8.0 + Vector Plugin**
2. **PGVector (PostgreSQL)**
3. **Milvus / Pinecone / Weaviate**
4. **简单方案：内存计算 + 缓存**

---

## 7. 实现步骤

### Phase 1: 数据库与实体 (Day 1)

1. 创建 `user_memory` 表
2. 创建 `UserMemory` 实体
3. 创建 `MemoryType` 枚举
4. 创建 `UserMemoryRepository`

### Phase 2: 核心服务 (Day 2)

1. 实现 `VectorService`（Embedding 调用）
2. 实现 `MemoryService`（CRUD + 检索）
3. 实现 `MemoryExtractionService`（提取 + 总结）

### Phase 3: 集成 (Day 3)

1. 集成到 `AIConversationService`
2. 实现 `ConversationEndDetector`
3. 测试完整流程

---

## 8. 关键设计决策

| 决策 | 理由 |
|-----|------|
| 三种类型（PROFILE/CAPABILITY/EVENT） | 覆盖用户信息、功能、事件，简单清晰 |
| 向量检索（相似度 >= 0.5） | 按需加载，避免每次加载所有记忆 |
| Conversation 结束 = 10分钟无消息 | 平衡准确性和实时性 |
| EVENT 按 conversationId 更新 | 同一对话的总结保持一致 |
| 同时检索三种类型 | 最大化上下文相关性 |

---

## 9. 详细实现步骤

### 现有代码结构

```
src/main/java/com/englishword/
├── entity/
│   └── AIConversation.java          ✅ 已存在
├── repository/
│   └── AIConversationRepository.java ✅ 已存在
├── service/
│   └── AIConversationService.java   ✅ 已存在（需要修改）
└── ...
```

### Step 1: 创建数据库表

**操作类型：** 🆕 新增

**执行 SQL：**

```sql
-- 在 english_word_app 数据库执行
CREATE TABLE user_memory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL COMMENT '用户ID',
    conversation_id VARCHAR(255) COMMENT '关联对话ID（EVENT专用）',
    memory_type VARCHAR(20) NOT NULL COMMENT '记忆类型: PROFILE/CAPABILITY/EVENT',
    content TEXT NOT NULL COMMENT '记忆内容',
    embedding BLOB COMMENT '向量（1536维 float32）',
    metadata JSON COMMENT '扩展字段',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_user_type (user_id, memory_type),
    INDEX idx_conversation (conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**还需要修改现有表：**

```sql
-- 给 ai_conversation 表添加 summarized 字段
ALTER TABLE ai_conversation
ADD COLUMN summarized TINYINT(1) DEFAULT 0 COMMENT '是否已总结为EVENT';
```

---

### Step 2: 创建实体类和枚举

**操作类型：** 🆕 新增

**文件 1: `entity/MemoryType.java`（新增）**

```java
package com.englishword.entity;

public enum MemoryType {
    PROFILE,      // 用户信息
    CAPABILITY,   // 用户创建的模式
    EVENT         // 对话事件总结
}
```

**文件 2: `entity/UserMemory.java`（新增）**

```java
package com.englishword.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_memory")
public class UserMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "conversation_id")
    private String conversationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "memory_type", nullable = false)
    private MemoryType memoryType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "BLOB")
    private byte[] embedding;

    @Column(columnDefinition = "JSON")
    private String metadata;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

---

### Step 3: 创建 Repository

**操作类型：** 🆕 新增

**文件: `repository/UserMemoryRepository.java`（新增）**

```java
package com.englishword.repository;

import com.englishword.entity.MemoryType;
import com.englishword.entity.UserMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserMemoryRepository extends JpaRepository<UserMemory, Long> {

    /**
     * 查询用户的所有记忆（按类型）
     */
    List<UserMemory> findByUserIdAndMemoryType(String userId, MemoryType type);

    /**
     * 查询对话的 EVENT
     */
    Optional<UserMemory> findByConversationIdAndMemoryType(String conversationId, MemoryType type);

    /**
     * 查询用户所有记忆（用于向量检索）
     */
    List<UserMemory> findByUserIdAndMemoryTypeIn(String userId, List<MemoryType> types);
}
```

---

### Step 4: 创建向量服务

**操作类型：** 🆕 新增

**文件: `service/VectorService.java`（新增）**

```java
package com.englishword.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

@Slf4j
@Service
public class VectorService {

    @Value("${ai.embedding.api-url:https://open.bigmodel.cn/api/paas/v4/embeddings}")
    private String apiUrl;

    @Value("${ai.chat.api-key}")
    private String apiKey;

    private final OkHttpClient httpClient = new OkHttpClient();

    /**
     * 调用 Embedding API 生成向量
     */
    public float[] embed(String text) {
        try {
            JSONObject body = new JSONObject();
            body.put("model", "embedding-3");
            body.put("input", text);

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(body.toJSONString(), MediaType.parse("application/json")))
                    .build();

            Response response = httpClient.newCall(request).execute();
            String responseBody = response.body().string();

            JSONObject json = JSONObject.parseObject(responseBody);
            JSONArray data = json.getJSONArray("data");
            JSONArray embeddingArray = data.getJSONObject(0).getJSONArray("embedding");

            float[] embedding = new float[embeddingArray.size()];
            for (int i = 0; i < embeddingArray.size(); i++) {
                embedding[i] = embeddingArray.getFloatValue(i);
            }

            return embedding;

        } catch (Exception e) {
            log.error("Embedding API 调用失败: {}", e.getMessage());
            return new float[0];
        }
    }

    /**
     * float[] 转 byte[] 用于存储
     */
    public byte[] floatArrayToBytes(float[] floats) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            for (float f : floats) {
                dos.writeFloat(f);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("向量转换失败", e);
            return new byte[0];
        }
    }

    /**
     * byte[] 转 float[] 用于计算
     */
    public float[] bytesToFloatArray(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return new float[0];
        }
        float[] floats = new float[bytes.length / 4];
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(bytes);
        for (int i = 0; i < floats.length; i++) {
            floats[i] = buffer.getFloat();
        }
        return floats;
    }

    /**
     * 计算余弦相似度
     */
    public float cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length || a.length == 0) {
            return 0f;
        }

        float dotProduct = 0;
        float normA = 0;
        float normB = 0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0 || normB == 0) {
            return 0f;
        }

        return dotProduct / (float) (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
```

---

### Step 5: 创建记忆服务

**操作类型：** 🆕 新增

**文件: `service/MemoryService.java`（新增）**

```java
package com.englishword.service;

import com.englishword.entity.MemoryType;
import com.englishword.entity.UserMemory;
import com.englishword.repository.UserMemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryService {

    private final UserMemoryRepository repository;
    private final VectorService vectorService;

    /**
     * 检索相关记忆
     */
    public MemoryContext retrieveMemories(String userId, String query) {
        float[] queryVector = vectorService.embed(query);

        // 获取用户所有记忆
        List<UserMemory> allMemories = repository.findByUserIdAndMemoryTypeIn(
                userId,
                List.of(MemoryType.PROFILE, MemoryType.CAPABILITY, MemoryType.EVENT)
        );

        // 计算相似度并筛选
        List<MemoryWithScore> scored = allMemories.stream()
                .map(m -> new MemoryWithScore(m, calculateSimilarity(m, queryVector)))
                .filter(m -> m.score >= 0.5f)
                .sorted(Comparator.comparingFloat(m -> -m.score)) // 降序
                .collect(Collectors.toList());

        // 按类型分组取 Top K
        List<UserMemory> profiles = scored.stream()
                .filter(m -> m.memory.getMemoryType() == MemoryType.PROFILE)
                .limit(3)
                .map(m -> m.memory)
                .collect(Collectors.toList());

        List<UserMemory> capabilities = scored.stream()
                .filter(m -> m.memory.getMemoryType() == MemoryType.CAPABILITY)
                .limit(2)
                .map(m -> m.memory)
                .collect(Collectors.toList());

        List<UserMemory> events = scored.stream()
                .filter(m -> m.memory.getMemoryType() == MemoryType.EVENT)
                .limit(3)
                .map(m -> m.memory)
                .collect(Collectors.toList());

        return new MemoryContext(profiles, capabilities, events);
    }

    private float calculateSimilarity(UserMemory memory, float[] queryVector) {
        float[] memoryVector = vectorService.bytesToFloatArray(memory.getEmbedding());
        return vectorService.cosineSimilarity(queryVector, memoryVector);
    }

    /**
     * 存储记忆
     */
    public void saveMemory(String userId, String conversationId,
                          MemoryType type, String content) {
        float[] embedding = vectorService.embed(content);
        byte[] embeddingBytes = vectorService.floatArrayToBytes(embedding);

        UserMemory memory = new UserMemory();
        memory.setUserId(userId);
        memory.setConversationId(conversationId);
        memory.setMemoryType(type);
        memory.setContent(content);
        memory.setEmbedding(embeddingBytes);

        repository.save(memory);
        log.info("存储记忆: type={}, content={}", type, content);
    }

    /**
     * 更新 EVENT（按 conversationId）
     */
    public void updateOrSaveEvent(String userId, String conversationId, String content) {
        Optional<UserMemory> existing = repository.findByConversationIdAndMemoryType(
                conversationId, MemoryType.EVENT);

        if (existing.isPresent()) {
            UserMemory memory = existing.get();
            float[] embedding = vectorService.embed(content);
            memory.setContent(content);
            memory.setEmbedding(vectorService.floatArrayToBytes(embedding));
            repository.save(memory);
            log.info("更新EVENT: conversationId={}", conversationId);
        } else {
            saveMemory(userId, conversationId, MemoryType.EVENT, content);
        }
    }

    /**
     * 记忆上下文 DTO
     */
    public record MemoryContext(
        List<UserMemory> profiles,
        List<UserMemory> capabilities,
        List<UserMemory> events
    ) {}

    private record MemoryWithScore(UserMemory memory, float score) {}
}
```

---

### Step 6: 创建记忆提取服务

**操作类型：** 🆕 新增

**文件: `service/MemoryExtractionService.java`（新增）**

```java
package com.englishword.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.englishword.client.ChatClient;
import com.englishword.entity.MemoryType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryExtractionService {

    private final ChatClient chatClient;
    private final MemoryService memoryService;

    private static final String EXTRACTION_PROMPT = """
        分析用户消息，判断是否需要存储记忆。

        记忆类型：
        - PROFILE: 用户关于自己的信息（身份、目标、能力、偏好）
        - CAPABILITY: 用户创建的模式或功能

        用户消息: %s

        如果需要存储，返回JSON：
        {"store": true, "type": "PROFILE或CAPABILITY", "content": "提取的内容"}

        如果不需要存储，返回：
        {"store": false}
        """;

    /**
     * 从对话中提取记忆
     */
    public void extractAndSave(String userId, String conversationId, String userMessage) {
        try {
            String prompt = String.format(EXTRACTION_PROMPT, userMessage);
            String result = chatClient.chat(null, prompt, "[]");

            JSONObject json = parseJson(result);
            if (json == null) return;

            if (json.getBooleanValue("store")) {
                String typeStr = json.getString("type");
                String content = json.getString("content");

                MemoryType type = MemoryType.valueOf(typeStr);
                memoryService.saveMemory(userId, conversationId, type, content);
            }

        } catch (Exception e) {
            log.error("提取记忆失败: {}", e.getMessage());
        }
    }

    /**
     * 对话结束时总结 EVENT
     */
    public void summarizeEvent(String userId, String conversationId, String messages) {
        try {
            String summaryPrompt = """
                总结这次对话的关键内容，用于记忆存储。
                用一句话概括：做了什么、学了什么、重要结果。

                对话内容：
                %s

                只返回总结内容，不要其他文字。
                """.formatted(messages);

            String summary = chatClient.chat(null, summaryPrompt, "[]");
            memoryService.updateOrSaveEvent(userId, conversationId, summary);

        } catch (Exception e) {
            log.error("总结EVENT失败: {}", e.getMessage());
        }
    }

    private JSONObject parseJson(String text) {
        try {
            // 尝试直接解析
            return JSON.parseObject(text);
        } catch (Exception e) {
            // 尝试提取 JSON 块
            int start = text.indexOf("{");
            int end = text.lastIndexOf("}");
            if (start >= 0 && end > start) {
                return JSON.parseObject(text.substring(start, end + 1));
            }
            return null;
        }
    }
}
```

---

### Step 7: 创建对话结束检测任务

**操作类型：** 🆕 新增

**文件: `job/ConversationEndDetector.java`（新增）**

```java
package com.englishword.job;

import com.englishword.entity.AIConversation;
import com.englishword.repository.AIConversationRepository;
import com.englishword.service.MemoryExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationEndDetector {

    private final AIConversationRepository conversationRepo;
    private final MemoryExtractionService extractionService;

    /**
     * 每5分钟检查一次
     */
    @Scheduled(fixedRate = 300000)
    public void detectEndedConversations() {
        log.debug("检查已结束的对话...");

        // 查找10分钟前更新且未总结的对话
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);

        // 需要在 Repository 添加这个方法
        List<AIConversation> ended = conversationRepo
                .findByUpdatedAtBeforeAndSummarizedFalse(threshold);

        for (AIConversation conv : ended) {
            try {
                extractionService.summarizeEvent(
                        conv.getUserId(),
                        conv.getConversationId(),
                        conv.getMessages()
                );

                // 标记已总结（需要在 AIConversation 实体添加字段）
                conv.setSummarized(true);
                conversationRepo.save(conv);

                log.info("对话已总结: conversationId={}", conv.getConversationId());

            } catch (Exception e) {
                log.error("总结对话失败: {}", e.getMessage());
            }
        }
    }
}
```

---

### Step 8: 修改 AIConversation 实体

**操作类型：** ✏️ 修改现有代码

**文件: `entity/AIConversation.java`**

**添加字段：**

```java
// 在 AIConversation 类中添加

/**
 * 是否已总结为 EVENT
 */
@Column(name = "summarized")
private Boolean summarized = false;
```

---

### Step 9: 修改 AIConversationRepository

**操作类型：** ✏️ 修改现有代码

**文件: `repository/AIConversationRepository.java`**

**添加方法：**

```java
// 在 AIConversationRepository 接口中添加

/**
 * 查找指定时间之前未总结的对话
 */
List<AIConversation> findByUpdatedAtBeforeAndSummarizedFalse(LocalDateTime threshold);
```

---

### Step 10: 修改 AIConversationService

**操作类型：** ✏️ 修改现有代码

**文件: `service/AIConversationService.java`**

**修改 `chat` 方法：**

```java
// 在类顶部添加依赖注入
private final MemoryService memoryService;
private final MemoryExtractionService extractionService;

// 修改 chat 方法
@Transactional
public ApiResponse<AIChatResponse> chat(String userId, AIChatRequest request) {
    try {
        String conversationId = request.getConversationId();
        // ... 现有代码 ...

        // ===== 新增：检索记忆 =====
        MemoryService.MemoryContext memoryContext =
            memoryService.retrieveMemories(userId, request.getMessage());

        // ===== 新增：构建带记忆的 systemPrompt =====
        String memoryPrompt = buildMemoryPrompt(memoryContext);

        // 3. 根据模式调用AI（传入 memoryPrompt）
        String aiReply;
        if ("word_training".equals(mode)) {
            aiReply = chatClient.practiceInScenario(
                    request.getTrainingWords(),
                    request.getScenario(),
                    request.getMessage(),
                    conversationHistory,
                    memoryPrompt  // 新增参数
            );
        } else {
            aiReply = chatClient.chat(
                    memoryPrompt,  // 替换 null
                    request.getMessage(),
                    conversationHistory
            );
        }

        // ... 现有保存对话历史的代码 ...

        // ===== 新增：提取记忆 =====
        extractionService.extractAndSave(userId, conversationId, request.getMessage());

        // ... 后续代码不变 ...

    } catch (Exception e) {
        // ...
    }
}

// ===== 新增方法 =====
private String buildMemoryPrompt(MemoryService.MemoryContext context) {
    StringBuilder sb = new StringBuilder();

    if (!context.profiles().isEmpty()) {
        sb.append("[用户信息]\n");
        context.profiles().forEach(p -> sb.append("• ").append(p.getContent()).append("\n"));
        sb.append("\n");
    }

    if (!context.capabilities().isEmpty()) {
        sb.append("[用户创建的模式]\n");
        context.capabilities().forEach(c -> sb.append("• ").append(c.getContent()).append("\n"));
        sb.append("\n");
    }

    if (!context.events().isEmpty()) {
        sb.append("[用户最近经历]\n");
        context.events().forEach(e -> sb.append("• ").append(e.getContent()).append("\n"));
        sb.append("\n");
    }

    if (sb.length() > 0) {
        sb.append("请根据以上信息个性化回复用户。\n");
    }

    return sb.length() > 0 ? sb.toString() : null;
}
```

**同样修改 `chatStream` 方法：**

```java
public Flux<String> chatStream(String userId, AIChatRequest request) {
    return Flux.create(emitter -> {
        try {
            // ... 现有代码 ...

            // ===== 新增：检索记忆 =====
            MemoryService.MemoryContext memoryContext =
                memoryService.retrieveMemories(userId, request.getMessage());
            String memoryPrompt = buildMemoryPrompt(memoryContext);

            // 修改调用
            String systemPrompt = getSystemPrompt(request.getMode(), request.getTrainingWords());

            // 合并 memoryPrompt 和 systemPrompt
            String finalSystemPrompt = memoryPrompt != null
                ? memoryPrompt + "\n" + systemPrompt
                : systemPrompt;

            chatClient.chatStream(
                    finalSystemPrompt,  // 使用合并后的
                    request.getMessage(),
                    // ... 其他参数 ...
            );

            // ... 在 onComplete 中添加记忆提取 ...
            @Override
            public void onComplete(String response) {
                try {
                    saveConversationHistory(finalConversation, request.getMessage(), response);

                    // ===== 新增：提取记忆 =====
                    extractionService.extractAndSave(
                        userId, finalConversationId, request.getMessage());

                    emitter.next("[DONE]");
                    emitter.complete();
                } finally {
                    UserContext.clearOperationUser();
                }
            }

        } catch (Exception e) {
            // ...
        }
    }, FluxSink.OverflowStrategy.BUFFER);
}
```

---

## 10. 实现步骤汇总

| Step | 操作 | 文件 | 说明 |
|------|------|------|------|
| 1 | 🆕 新增 | SQL | 创建 `user_memory` 表，修改 `ai_conversation` 表 |
| 2 | 🆕 新增 | `entity/MemoryType.java` | 记忆类型枚举 |
| 2 | 🆕 新增 | `entity/UserMemory.java` | 记忆实体 |
| 3 | 🆕 新增 | `repository/UserMemoryRepository.java` | 记忆数据访问 |
| 4 | 🆕 新增 | `service/VectorService.java` | 向量计算服务 |
| 5 | 🆕 新增 | `service/MemoryService.java` | 记忆管理服务 |
| 6 | 🆕 新增 | `service/MemoryExtractionService.java` | 记忆提取服务 |
| 7 | 🆕 新增 | `job/ConversationEndDetector.java` | 对话结束检测 |
| 8 | ✏️ 修改 | `entity/AIConversation.java` | 添加 `summarized` 字段 |
| 9 | ✏️ 修改 | `repository/AIConversationRepository.java` | 添加查询方法 |
| 10 | ✏️ 修改 | `service/AIConversationService.java` | 集成记忆检索和提取 |

---

## 11. 注意事项

1. **向量检索简化方案**：当前使用内存计算相似度，如果记忆数量超过1000条，建议改用 Milvus 或 PGVector

2. **ChatClient 修改**：需要确认 `ChatClient.practiceInScenario()` 方法是否支持额外参数，如不支持需要修改

3. **启用定时任务**：需要在主类添加 `@EnableScheduling` 注解

4. **API Key 配置**：确保 `application.yml` 中已配置智谱 AI 的 Embedding API
