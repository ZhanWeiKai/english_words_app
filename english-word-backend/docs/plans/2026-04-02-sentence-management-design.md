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

---

## 数据库迁移版本管理（Flyway）

本项目使用 **Flyway** 进行数据库版本管理，迁移文件打包在 JAR 中，应用启动时自动执行。

### 文件命名规范

```
V{版本号}__{描述}.sql
```

- 版本号：递增整数（1, 2, 3...），不可重复
- 描述：下划线分隔的英文描述
- 双下划线 `__` 分隔版本号和描述

### 现有迁移文件

| 文件名 | 说明 |
|-------|------|
| `V1__init_schema.sql` | 初始化数据库表结构（user, word, training_session, ai_conversation） |
| `V2__add_sentence_table.sql` | **新增** - 添加 sentence 表 |

### 本地文件位置

```
english-word-backend/
└── src/
    └── main/
        └── resources/
            └── db/
                └── migration/
                    ├── V1__init_schema.sql      ← 已存在
                    └── V2__add_sentence_table.sql  ← 新增
```

### 服务器部署

**重要**：SQL 文件打包在 JAR 内，无需单独上传到服务器！

```
┌─────────────────────────────────────────────────────────────┐
│  开发流程                                                    │
├─────────────────────────────────────────────────────────────┤
│  1. 本地创建 V2__add_sentence_table.sql                     │
│  2. mvn clean package → SQL 文件打包进 JAR                   │
│  3. 上传 JAR 到服务器                                        │
│  4. Docker 重启 → Flyway 自动检测并执行新迁移                │
└─────────────────────────────────────────────────────────────┘
```

### 服务器上的 Flyway 状态

Flyway 在数据库中维护 `flyway_schema_history` 表，记录已执行的迁移：

```sql
-- 查看已执行的迁移
SELECT * FROM flyway_schema_history ORDER BY installed_rank;

-- 示例输出：
-- | installed_rank | version | description          | success |
-- |----------------|---------|----------------------|---------|
-- | 1              | 1       | init schema          | 1       |
-- | 2              | 2       | add sentence table   | 1       |
```

### 版本管理规则

| 规则 | 说明 |
|-----|------|
| ✅ 版本号递增 | 新迁移版本号必须大于已有版本号 |
| ❌ 不可修改已有文件 | 已执行的迁移文件不能修改 |
| ✅ 向后兼容 | 新增表/列，避免删除/修改已有结构 |
| ✅ 幂等性 | 使用 `IF NOT EXISTS`、`IF EXISTS` 等语句 |

### 部署命令回顾

```bash
# 1. 本地编译（SQL 文件自动打包进 JAR）
cd english-word-backend
mvn clean package -DskipTests

# 2. 上传 JAR 到服务器
scp target/english-word-backend-1.0.0.jar ubuntu@119.91.206.195:/root/english-word-app/target/

# 3. 重启 Docker（Flyway 自动执行新迁移）
ssh ubuntu@119.91.206.195 "cd /root/english-word-app && docker compose restart app"

# 4. 验证迁移执行成功
ssh ubuntu@119.91.206.195 "docker compose -f /root/english-word-app/docker-compose.yml logs app | grep -i flyway"
```

---

## API Design

### 方案：通过 MCP Tool 管理句子（与 Word 一致）

参考 `WordTool.java` 的实现方式，句子功能通过 MCP Tool 供 AI 调用，而不是 REST API。

用户在 AI Chat 中说"收藏这个句子"，AI 自动调用 `add_sentence` 工具完成保存。

### MCP Tools 定义

#### 1. add_sentence - 添加句子

```java
@McpTool(name = "add_sentence", description = """
    保存一个有价值的英语句子到用户句库。

    【重要】调用此工具前，AI 必须提供以下信息：
    - englishText: 英文句子原文
    - chineseText: 中文翻译（AI 必须翻译）
    - markedWords: 句子中的重点词汇（可选）

    使用场景：用户说"收藏这个句子"、"保存这句话"时调用。
    """)
public String addSentence(
    @McpParam(name = "englishText", description = "英文句子") String englishText,
    @McpParam(name = "chineseText", description = "中文翻译（AI必须提供）") String chineseText,
    @McpParam(name = "markedWords", description = "重点词汇，逗号分隔（可选）", required = false) String markedWords
)
```

#### 2. list_sentences - 列出句子

```java
@McpTool(name = "list_sentences", description = "获取当前用户的句子列表，支持分页和搜索")
public String listSentences(
    @McpParam(name = "keyword", description = "搜索关键词（英文/中文/标记词）", required = false) String keyword,
    @McpParam(name = "page", description = "页码，从0开始", required = false) Integer page,
    @McpParam(name = "size", description = "每页数量，默认20", required = false) Integer size
)
```

#### 3. delete_sentence - 删除句子

```java
@McpTool(name = "delete_sentence", description = "从用户句库中删除一个句子")
public String deleteSentence(
    @McpParam(name = "sentenceId", description = "句子ID") String sentenceId
)
```

#### 4. get_sentence_statistics - 获取统计

```java
@McpTool(name = "get_sentence_statistics", description = "获取当前用户句库的统计信息")
public String getSentenceStatistics()
```

### 与 REST API 的区别

| 方面 | Word（已有） | Sentence（本设计） |
|-----|-------------|-------------------|
| 管理方式 | MCP Tool | MCP Tool（一致） |
| REST API | 无 | 无 |
| 用户操作 | AI Chat 中说"添加单词" | AI Chat 中说"收藏句子" |
| 数据库 | MySQL + JSON 字段 | MySQL + JSON 字段 |

## Architecture

### Backend Files

| 文件 | 操作 | 说明 |
|-----|------|------|
| `Sentence.java` | **新增** | JPA 实体类，包含 UUID 生成、自动时间戳 |
| `SentenceRepository.java` | **新增** | JPA Repository，包含自定义搜索查询 |
| `SentenceService.java` | **新增** | 业务逻辑：CRUD + 搜索 |
| `SentenceTool.java` | **新增** | MCP Tool 定义（参考 WordTool.java） |
| `V2__add_sentence_table.sql` | **新增** | Flyway 数据库迁移 |

**注意：不需要 SentenceController.java，因为通过 MCP Tool 而非 REST API 管理数据。**

### Android Files

| 文件 | 操作 | 说明 |
|-----|------|------|
| `WordVaultScreen.kt` | **修改** | 添加 Tab 切换逻辑，调整布局 |
| `SentenceListScreen.kt` | **新增** | 句子列表页面 |
| `SentenceCard.kt` | **新增** | 句子卡片组件 |
| `SentenceViewModel.kt` | **新增** | 句子列表状态管理 |

**注意：**
- **不需要** `SentenceApiService.kt` - 句子通过 AI Chat + MCP Tool 管理
- **不需要** `SaveSentenceDialog.kt` - 用户直接对 AI 说"收藏这个句子"
- **不需要** 修改 `RetrofitClient.kt`

---

## MCP Tool Response Format

### add_sentence 成功响应

```json
{
  "success": true,
  "message": "句子已保存",
  "sentence": {
    "sentenceId": "uuid-xxx",
    "englishText": "The apple is very delicious",
    "chineseText": "这个苹果非常美味",
    "markedWords": ["apple", "delicious"],
    "createdAt": "2026-04-02T10:30:00"
  }
}
```

### list_sentences 响应

```json
{
  "success": true,
  "totalElements": 25,
  "totalPages": 3,
  "currentPage": 0,
  "sentences": [
    {
      "sentenceId": "uuid-xxx",
      "englishText": "The apple is very delicious",
      "chineseText": "这个苹果非常美味",
      "markedWords": [
        {"word": "apple", "wordId": null},
        {"word": "delicious", "wordId": "uuid-yyy"}
      ],
      "createdAt": "2026-04-02T10:30:00"
    }
  ]
}
```

---

## Implementation Order

### Phase 1: Backend（后端）

1. **数据库迁移** - 创建 `V2__add_sentence_table.sql`
2. **实体类** - `Sentence.java`
3. **Repository** - `SentenceRepository.java`
4. **Service** - `SentenceService.java`
5. **MCP Tool** - `SentenceTool.java`（参考 `WordTool.java`）
6. **测试** - 在 AI Chat 中说"收藏这个句子"测试

### Phase 2: Android UI

1. **Tab 切换** - 修改 `WordVaultScreen.kt`
2. **句子卡片** - `SentenceCard.kt`
3. **句子列表** - `SentenceListScreen.kt`
4. **ViewModel** - `SentenceViewModel.kt`（调用现有 sentence 列表 API）

### Design Decisions

1. **JSON field for marked_words** over join table - consistent with existing project patterns (ai_conversation.messages, training_session.word_ids already use JSON), simpler for small per-user data volumes
2. **AI chat driven** - users manage sentences through AI conversation, not a traditional CRUD form
3. **Dynamic linking** - word associations upgrade from plain text to vault link when words are added later

---

## Frontend Design

### Entry Point: Vault Page with Tabs

将 Vault 页面改造为双 Tab 结构，布局调整如下：

```
┌─────────────────────────────────────────────┐
│  Welcome, username!       [Refresh][Logout]  ← TopAppBar
├─────────────────────────────────────────────┤
│  ┌────────────────┐┌────────────────┐       │
│  │     Words      ││   Sentences    │       │ ← Tab 占满整行，文字居中
│  └────────────────┘└────────────────┘       │
├─────────────────────────────────────────────┤
│                                             │
│  Tab 内容区域                                │
│                                             │
└─────────────────────────────────────────────┘
```

**改动说明：**
- TopAppBar 的 title 从 "Word Vault" 改为 "Welcome, {username}!"
- 原 Welcome 位置替换为 Tab Row（Words / Sentences）
- Tab Row 占满整行宽度，每个 Tab 平分宽度，文字居中
- Tab 切换时，筛选器区域也会变化

### Tab 1: Words (现有功能)

保持现有的单词列表功能，包含搜索框和掌握程度筛选器：

```
┌─────────────────────────────────────────────┐
│  [🔍 Search words...]                        │
│  [All] [Learning] [Mastered]                 ← 掌握程度筛选
├─────────────────────────────────────────────┤
│  单词列表...                                  │
└─────────────────────────────────────────────┘
```

### Tab 2: Sentences (新增)

只有搜索框，无筛选器：

```
┌─────────────────────────────────────────────┐
│  [🔍 Search sentences...]                    │
├─────────────────────────────────────────────┤
│  句子列表...                                  │
└─────────────────────────────────────────────┘
```

#### 句子列表视图

```
┌─────────────────────────────────────────────┐
│  [🔍 Search sentences...]        [+ Add]    │
├─────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────┐│
│  │ The apple is very delicious            ││
│  │ 这个苹果非常美味                         ││
│  │ 📝 apple, delicious                     ││
│  │                          2026-04-02  🗑️ ││
│  └─────────────────────────────────────────┘│
│                                             │
│  ┌─────────────────────────────────────────┐│
│  │ I want to be a brave person            ││
│  │ 我想成为一个勇敢的人                     ││
│  │ 📝 brave → (linked to word vault)       ││
│  │                          2026-04-01  🗑️ ││
│  └─────────────────────────────────────────┘│
│                                             │
│  ◀ 1 2 3 ... ▶                             │
└─────────────────────────────────────────────┘
```

#### 列表项设计

每个句子卡片包含：

| 元素 | 说明 |
|-----|------|
| 英文句子 | 主要内容，点击可展开详情 |
| 中文翻译 | 灰色小字 |
| 标记词标签 | 蓝色 chip，如果关联了词库则显示链接图标 |
| 创建日期 | 右下角灰色小字 |
| 删除按钮 | 右侧垃圾桶图标 |

#### 搜索功能

- 搜索框在顶部
- 支持搜索英文、中文、标记词
- 实时搜索（输入时自动过滤）

### 句子来源：AI Chat（通过 MCP Tool）

用户在 AI Chat 中看到有用的句子时，直接对 AI 说：

```
用户: "收藏这个句子"
或
用户: "把刚才那个例句保存到句库"
或
用户: "这句话很有用，帮我收藏一下"
```

AI 会自动调用 `add_sentence` MCP Tool 完成收藏：

```
AI: 好的，我已经帮你收藏了这个句子：

    "Smartphones have become ubiquitous in modern society."
    智能手机在现代社会已经无处不在。

    标记词：ubiquitous
    已保存到你的句库！
```

#### 与 Word 一致的操作体验

| 操作 | 用户说法 | AI 调用的工具 |
|-----|---------|--------------|
| 添加单词 | "添加 apple 到词库" | `add_word` |
| 查询单词 | "我有哪些单词" | `list_user_words` |
| 删除单词 | "删除这个单词" | `delete_word` |
| **收藏句子** | **"收藏这个句子"** | **`add_sentence`** |
| **查看句子** | **"我收藏了哪些句子"** | **`list_sentences`** |
| **删除句子** | **"删除这个句子"** | **`delete_sentence`** |

### 新增/修改的 Android 文件

| 文件 | 操作 | 说明 |
|-----|------|------|
| `WordVaultScreen.kt` | **修改** | 添加 Tab 切换逻辑，调整布局 |
| `SentenceListScreen.kt` | **新增** | 句子列表页面 |
| `SentenceCard.kt` | **新增** | 句子卡片组件 |
| `SentenceViewModel.kt` | **新增** | 句子列表状态管理 |
| `SentenceApiService.kt` | **新增** | 调用 `GET /api/sentences` 获取列表（仅用于展示） |

**注意：**
- **不需要** 修改 `AIChatScreen.kt` - 用户直接对 AI 说"收藏句子"
- **不需要** `SaveSentenceDialog.kt` - 收藏通过 MCP Tool 完成
- `SentenceApiService.kt` **仅用于列表展示**，增删改通过 AI Chat + MCP Tool

### Tab 切换实现

```kotlin
// VaultScreen.kt
enum class VaultTab { WORDS, SENTENCES }

@Composable
fun VaultScreen() {
    var selectedTab by remember { mutableStateOf(VaultTab.WORDS) }

    Column {
        // Tab Row
        TabRow(selectedTabIndex = selectedTab.ordinal) {
            Tab(
                selected = selectedTab == VaultTab.WORDS,
                onClick = { selectedTab = VaultTab.WORDS },
                text = { Text("Words") }
            )
            Tab(
                selected = selectedTab == VaultTab.SENTENCES,
                onClick = { selectedTab = VaultTab.SENTENCES },
                text = { Text("Sentences") }
            )
        }

        // Tab Content
        when (selectedTab) {
            VaultTab.WORDS -> WordListContent()
            VaultTab.SENTENCES -> SentenceListContent()
        }
    }
}
```

### Interaction Flow

```
用户在 AI Chat 看到有用的句子
       ↓
用户: "收藏这个句子"
       ↓
AI 调用 add_sentence MCP Tool
       ↓
后端保存句子到数据库
       ↓
AI 回复: "已保存到你的句库！"
       ↓
用户切换到 Vault → Sentences Tab 查看
```

**与 Word 操作完全一致，无需额外 UI 按钮。**
