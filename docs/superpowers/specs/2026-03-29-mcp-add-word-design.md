# MCP Add Word 设计文档

## 概述

为 English Word App 添加 MCP (Model Context Protocol) 服务，允许 Android App 的 AI 聊天识别"添加单词"意图后，自动调用 MCP 工具添加单词到用户单词库。

## 技术选型

使用 **Spring AI MCP** 实现，相比原生 MCP SDK 更简洁：

| 方案 | 优点 | 缺点 |
|------|------|------|
| ~~MCP Java SDK~~ | 官方SDK | API复杂，需要手动处理JsonSchema |
| **Spring AI MCP** ✅ | 注解驱动，代码简洁 | 需要Spring AI依赖 |

## 架构

```
┌─────────────────┐      MCP Protocol (SSE)    ┌─────────────────┐
│   Android App   │ ◄────────────────────────► │  Backend Server │
│  (MCP Client)   │         :8885/sse          │  (MCP Server)   │
│                 │                            │                 │
│  启动时连接 MCP  │   调用工具 add_word        │  @Tool 注解     │
│  AI Chat 识别    │ ────────────────────────► │  自动注册工具   │
│  "帮我记下apple" │                            │  调用智谱AI填充 │
│                 │ ◄────────────────────────  │  保存到数据库   │
└─────────────────┘                            └─────────────────┘
```

## 工具定义

### add_word

**描述**: 添加单词到用户单词库，自动调用 AI 填充详细信息

**参数**:
| 参数名 | 类型 | 必需 | 描述 |
|--------|------|------|------|
| word | string | 是 | 要添加的英文单词或词组 |
| translation | string | 否 | 中文翻译，不提供则自动生成 |

**返回**: 添加成功的单词完整信息

## 工具内部流程

```
输入: word="apple", translation="苹果"
        │
        ▼
┌─────────────────────────────────────┐
│  Step 1: 调用智谱 AI 填充信息        │
│  生成:                              │
│  - pronunciation (音标)             │
│  - partOfSpeech (词性)              │
│  - definition (英文释义)            │
│  - exampleSentence (例句)           │
│  - exampleTranslation (例句翻译)    │
└─────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────┐
│  Step 2: 构建完整 Word 对象         │
│  + userId (从 JWT 获取)             │
│  + status: "LEARNING"               │
│  + masteryLevel: 1                  │
└─────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────┐
│  Step 3: 调用 WordService.addWord   │
│  保存到数据库                        │
└─────────────────────────────────────┘
        │
        ▼
    返回成功结果
```

## 后端实现 (Spring AI 方式)

### 依赖

```xml
<!-- Spring AI BOM -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- MCP Server for WebMVC -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
</dependency>
```

### 文件结构

```
english-word-backend/src/main/java/com/englishword/
├── mcp/
│   └── WordTool.java            # @Tool 注解定义工具
├── service/
│   └── WordInfoGenerator.java   # 调用智谱AI生成单词信息
└── config/
    └── McpConfig.java           # 注册工具到MCP Server
```

### 核心代码示例

**WordTool.java** - 使用 @Tool 注解：
```java
@Component
public class WordTool {

    @Tool(description = "添加单词到用户单词库，自动调用AI填充详细信息")
    public String addWord(
        @ToolParam(description = "要添加的英文单词") String word,
        @ToolParam(description = "中文翻译（可选）", required = false) String translation) {
        // 实现逻辑
    }
}
```

**McpConfig.java** - 注册工具：
```java
@Bean
public ToolCallbackProvider registMCPTools(WordTool wordTool) {
    return MethodToolCallbackProvider.builder()
            .toolObjects(wordTool)
            .build();
}
```

### MCP 端点

默认端点: `http://localhost:8885/sse`

可在 `application.yml` 中配置:
```yaml
spring:
  ai:
    mcp:
      server:
        sse-endpoint: /sse
```

## Android 端实现

### 连接流程

```
App 启动
    │
    ▼
用户登录成功
    │
    ▼
McpClient.initialize()
    │
    ├─ 连接 http://47.83.126.42:8885/sse
    ├─ 携带 JWT Token
    └─ 获取工具列表
    │
    ▼
用户进入 AI Chat
    │
    ├─ 智谱 AI 获得工具描述
    │
    ▼
用户: "帮我记下apple"
    │
    ├─ 智谱 AI 返回 tool_call: add_word
    │
    ▼
McpClient.callTool("add_word", {word: "apple"})
    │
    ▼
AI 生成回复: "已添加apple到你的单词库"
```

## 认证

MCP Server 复用现有的 JWT 认证机制：

1. Android 客户端在 MCP 连接时携带 `Authorization: Bearer <token>`
2. MCP Server 从 Token 中提取 `userId`
3. 工具调用时使用该 `userId` 保存单词

## 错误处理

| 错误场景 | 处理方式 |
|----------|----------|
| Token 无效/过期 | 返回 401，客户端重新登录 |
| 单词已存在 | 返回提示 "单词已存在" |
| 智谱 AI 调用失败 | 返回错误 "无法获取单词信息，请稍后重试" |
| 数据库保存失败 | 返回 500 错误 |
