# MCP Add Word 设计文档

## 概述

为 English Word App 添加 MCP (Model Context Protocol) 服务，允许 Android App 的 AI 聊天识别"添加单词"意图后，自动调用 MCP 工具添加单词到用户单词库。

## 架构

```
┌─────────────────┐      MCP Protocol (SSE)    ┌─────────────────┐
│   Android App   │ ◄────────────────────────► │  Backend Server │
│  (MCP Client)   │         :8885/api/mcp      │  (MCP Server)   │
│                 │                            │                 │
│  启动时连接 MCP  │   调用工具 add_word        │  Tool: add_word │
│  AI Chat 识别    │ ────────────────────────► │  1. 调用智谱AI  │
│  "帮我记下apple" │                            │  2. 填充信息    │
│                 │ ◄────────────────────────  │  3. 保存数据库  │
└─────────────────┘                            └─────────────────┘
```

## MCP Server 配置

| 配置项 | 值 |
|--------|-----|
| 端点 | `/api/mcp` |
| 协议 | SSE (Server-Sent Events) |
| 协议版本 | MCP 2024-11-05 |
| 认证 | JWT Token (复用现有) |

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

## 后端实现

### 新增文件

```
english-word-backend/src/main/java/com/englishword/
├── mcp/
│   ├── McpController.java       # MCP SSE 端点
│   ├── McpService.java          # MCP 协议处理
│   └── McpToolsService.java     # 工具实现
└── config/
    └── McpConfig.java           # MCP 配置
```

### 依赖

```xml
<dependency>
    <groupId>io.modelcontextprotocol</groupId>
    <artifactId>mcp-server-spring-webmvc</artifactId>
    <version>0.5.0</version>
</dependency>
```

## Android 端实现

### 新增文件

```
android-word-app/app/src/main/java/com/englishword/
├── mcp/
│   ├── McpClient.kt            # MCP 客户端连接
│   └── McpToolExecutor.kt      # 工具调用封装
```

### 修改文件

- `AIChatViewModel.kt` - 集成 MCP 工具调用
- `MainActivity.kt` - 登录后初始化 MCP 连接

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
    ├─ 连接 http://47.83.126.42:8885/api/mcp
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
