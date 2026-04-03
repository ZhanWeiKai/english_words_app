# 流式 MCP 工具调用实现设计

## 目标

让流式接口 `/api/ai/chat/stream` 支持 MCP 工具调用，当前只有非流式接口支持。

## 当前状态

| 接口 | MCP 工具支持 | 调用方法 |
|-----|-------------|---------|
| `POST /api/ai/chat` (非流式) | ✅ 支持 | `callApiWithTools()` |
| `POST /api/ai/chat/stream` (流式) | ❌ 不支持 | `chatStreamSimple()` |

## 实现方案：混合模式

**思路**：流式输出文本内容，当检测到需要工具调用时：
1. 通知前端"正在调用工具"
2. 执行工具调用
3. 继续流式输出最终结果

**用户体验**：
```
用户发送消息
  ↓
流式输出文本...（如果有工具调用）
  ↓
[UI 显示: 正在查询天气...] ← 新增
  ↓
执行 MCP 工具
  ↓
继续流式输出最终回复...
  ↓
完成
```

---

## 修改清单

### 1. ChatClient.java - 接口定义

**文件路径**: `src/main/java/com/englishword/client/ChatClient.java`

**修改类型**: 添加新逻辑

**改动内容**:
```java
// 在 StreamCallback 接口中添加新方法（第 15-30 行之间）
interface StreamCallback {
    void onChunk(String chunk);
    void onComplete(String fullResponse);
    void onError(String error);

    // 【新增】工具调用通知
    default void onToolCall(String toolName, String arguments) {
        // 默认空实现，保持向后兼容
    }
}
```

---

### 2. OpenAICompatibleClient.java - 核心实现

**文件路径**: `src/main/java/com/englishword/client/OpenAICompatibleClient.java`

#### 2.1 chatStream 方法（第 327-336 行）

**修改类型**: 修改原有逻辑

**改动前**:
```java
public void chatStream(String systemPrompt, String userMessage,
                       String conversationHistory, StreamCallback callback) {
    try {
        // 目前流式模式不支持工具调用，直接使用简单流式
        chatStreamSimple(systemPrompt, userMessage, conversationHistory, callback);
    } catch (Exception e) {
        ...
    }
}
```

**改动后**:
```java
public void chatStream(String systemPrompt, String userMessage,
                       String conversationHistory, StreamCallback callback) {
    try {
        // 【修改】支持 MCP 工具调用
        if (mcpReady && mcpFunctions != null && !mcpFunctions.isEmpty()) {
            chatStreamWithTools(systemPrompt, userMessage, conversationHistory, callback);
        } else {
            chatStreamSimple(systemPrompt, userMessage, conversationHistory, callback);
        }
    } catch (Exception e) {
        ...
    }
}
```

#### 2.2 新增 chatStreamWithTools 方法

**修改类型**: 添加新逻辑

**位置**: 在 `chatStreamSimple` 方法之后（约第 420 行之后）

**新增代码**:
```java
/**
 * 支持工具调用的流式方法
 *
 * 流程：
 * 1. 发送带 tools 参数的流式请求
 * 2. 收集完整响应
 * 3. 检查是否有 tool_calls
 * 4. 如果有，执行工具，通知前端，然后继续流式请求
 * 5. 如果没有，直接流式输出内容
 */
private void chatStreamWithTools(String systemPrompt, String userMessage,
                                  String conversationHistory, StreamCallback callback) {
    executorService.submit(() -> {
        try {
            JSONArray messages = buildMessages(systemPrompt, userMessage, conversationHistory);
            processStreamWithTools(messages, callback, 0);
        } catch (Exception e) {
            log.error("[Stream-MCP] Error", e);
            callback.onError(e.getMessage());
        }
    });
}

/**
 * 递归处理流式请求，支持多轮工具调用
 */
private void processStreamWithTools(JSONArray messages, StreamCallback callback, int iteration) {
    if (iteration >= 5) {
        callback.onError("工具调用次数超过限制");
        return;
    }

    try {
        // 构建带 tools 的请求（非流式，获取完整响应来判断是否需要工具）
        JSONObject requestBody = buildRequestBody(null, null, null, messages);
        requestBody.put("tools", mcpFunctions);
        // 注意：这里用非流式请求来检测工具调用

        JSONObject response = sendRequest(requestBody);
        JSONObject message = response.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message");

        JSONArray toolCalls = message.getJSONArray("tool_calls");

        if (toolCalls != null && !toolCalls.isEmpty()) {
            // 有工具调用
            messages.add(message);

            for (int i = 0; i < toolCalls.size(); i++) {
                JSONObject toolCall = toolCalls.getJSONObject(i);
                String toolCallId = toolCall.getString("id");
                JSONObject function = toolCall.getJSONObject("function");
                String functionName = function.getString("name");
                String argumentsStr = function.getString("arguments");

                // 【关键】通知前端工具调用
                callback.onToolCall(functionName, argumentsStr);

                // 执行工具
                JSONObject arguments = JSON.parseObject(argumentsStr);
                JSONObject toolResult = mcpClient.callTool(functionName, arguments)
                        .get(config.getTimeout(), TimeUnit.SECONDS);
                String toolOutput = extractToolContent(toolResult);

                // 添加工具结果
                JSONObject toolMessage = new JSONObject();
                toolMessage.put("role", "tool");
                toolMessage.put("tool_call_id", toolCallId);
                toolMessage.put("content", toolOutput);
                messages.add(toolMessage);
            }

            // 递归处理下一轮（这一轮用流式输出最终回复）
            processStreamWithTools(messages, callback, iteration + 1);

        } else {
            // 没有工具调用，流式输出最终内容
            String content = message.getString("content");
            if (content != null && !content.isEmpty()) {
                // 模拟流式输出效果
                streamOutContent(content, callback);
            }
            callback.onComplete(content);
        }
    } catch (Exception e) {
        log.error("[Stream-MCP] Error in iteration {}", iteration, e);
        callback.onError(e.getMessage());
    }
}

/**
 * 将内容分块流式输出（模拟打字效果）
 */
private void streamOutContent(String content, StreamCallback callback) {
    // 按句子或固定长度分块
    int chunkSize = 5;  // 每次输出5个字符
    for (int i = 0; i < content.length(); i += chunkSize) {
        int end = Math.min(i + chunkSize, content.length());
        String chunk = content.substring(i, end);
        callback.onChunk(chunk);
        // 可选：添加小延迟模拟打字效果
    }
}
```

---

### 3. AIConversationService.java - 服务层

**文件路径**: `src/main/java/com/englishword/service/AIConversationService.java`

#### 3.1 chatStream 方法的 FluxEmitter 处理

**修改类型**: 修改原有逻辑

**位置**: `chatStream` 方法中的 Flux.create 部分

**改动内容**:
```java
// 在 Flux.create 的 emitter 中处理新的 onToolCall 回调
chatClient.chatStream(systemPrompt, message, history, new ChatClient.StreamCallback() {
    @Override
    public void onChunk(String chunk) {
        emitter.next(chunk);
    }

    @Override
    public void onComplete(String response) {
        saveConversationHistory(...);
        emitter.next("[DONE]");
        emitter.complete();
    }

    @Override
    public void onError(String error) {
        emitter.error(new RuntimeException(error));
    }

    // 【新增】处理工具调用
    @Override
    public void onToolCall(String toolName, String arguments) {
        // 发送工具调用事件给前端
        JSONObject toolEvent = new JSONObject();
        toolEvent.put("type", "tool_call");
        toolEvent.put("tool", toolName);
        toolEvent.put("arguments", arguments);
        emitter.next("__TOOL_CALL__:" + toolEvent.toJSONString());
    }
});
```

---

### 4. AIController.java - 控制器层

**文件路径**: `src/main/java/com/englishword/controller/AIController.java`

**修改类型**: 修改原有逻辑

**位置**: `chatStream` 方法的 SSE 事件映射

**改动内容**:
```java
@PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<String>> chatStream(...) {
    return aiConversationService.chatStream(userId, request)
            .map(chunk -> {
                // 【新增】处理工具调用事件
                if (chunk.startsWith("__TOOL_CALL__:")) {
                    String toolJson = chunk.substring(14);
                    return ServerSentEvent.<String>builder()
                            .event("tool_call")
                            .data(toolJson)
                            .build();
                }
                // 原有逻辑...
                if (chunk.startsWith("__CONVERSATION_ID__:")) {
                    return ServerSentEvent.<String>builder()
                            .event("conversationId")
                            .data(chunk.substring(20))
                            .build();
                }
                if ("[DONE]".equals(chunk)) {
                    return ServerSentEvent.<String>builder()
                            .event("done")
                            .data("[DONE]")
                            .build();
                }
                return ServerSentEvent.<String>builder()
                        .event("message")
                        .data(chunk)
                        .build();
            });
}
```

---

## SSE 事件类型总结

实现后，前端会收到以下 SSE 事件：

| event | data | 说明 |
|-------|------|------|
| `conversationId` | `uuid` | 对话ID |
| `tool_call` | `{"tool":"get_weather","arguments":"..."}` | 【新增】工具调用中 |
| `message` | `文本内容` | 文本块 |
| `done` | `[DONE]` | 完成 |

---

## 前端配合

前端需要处理新的 `tool_call` 事件：

```kotlin
// SSE 事件处理
when (event) {
    "conversationId" -> { /* 保存对话ID */ }
    "tool_call" -> {
        // 解析 data 获取工具名
        val toolJson = JSONObject(data)
        val toolName = toolJson.getString("tool")
        // 显示 UI 提示："正在调用 $toolName..."
        showToolCallHint(toolName)
    }
    "message" -> { /* 追加文本 */ }
    "done" -> { /* 隐藏工具调用提示，完成 */ }
}
```

---

## 风险和注意事项

1. **工具调用阶段会"暂停"流式输出** - 这是不可避免的，前端需要显示 loading 提示
2. **多轮工具调用** - 最多 5 轮，避免无限循环
3. **超时处理** - 工具调用有超时限制（使用 config.getTimeout()）
4. **向后兼容** - StreamCallback.onToolCall() 使用 default 方法，不影响现有实现

---

## 实现顺序

1. ✅ ChatClient.java - 添加 onToolCall 方法
2. ✅ OpenAICompatibleClient.java - 添加 chatStreamWithTools 方法
3. ✅ AIConversationService.java - 处理 onToolCall 回调
4. ✅ AIController.java - 处理 SSE 事件映射
5. ✅ 编译测试
6. ✅ 部署验证
