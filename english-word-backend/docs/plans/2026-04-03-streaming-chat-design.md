# AI 聊天流式输出实现计划

## 概述

### 目标
将 AI 聊天从"等待完整响应后显示"改为"流式输出，打字机效果"。

### 技术方案
- 使用 SSE (Server-Sent Events) 实现流式传输
- 保留原有非流式接口，新增流式接口
- Android 端使用 OkHttp SSE 接收流式数据

### 接口策略
- **默认使用流式** - Android 端优先调用 `/api/ai/chat/stream`
- **保留非流式** - 原有 `/api/ai/chat` 接口保留，作为备用

---

## 后端改动

### 1. OpenAICompatibleClient.java - 新增流式调用方法

**文件**: `src/main/java/com/englishword/client/OpenAICompatibleClient.java`

**改动内容**:
```java
// 新增接口定义
public interface StreamCallback {
    void onChunk(String chunk);
    void onComplete(String fullResponse);
    void onError(String error);
}

// 新增流式调用方法
public void chatStream(String systemPrompt, String userMessage,
                       String conversationHistory, StreamCallback callback) {
    JSONObject requestBody = buildRequestBody(systemPrompt, userMessage, conversationHistory, null);
    requestBody.put("stream", true);  // 开启流式

    Request request = new Request.Builder()
        .url(config.getApiUrl())
        .addHeader("Authorization", "Bearer " + config.getApiKey())
        .addHeader("Content-Type", "application/json")
        .addHeader("Accept", "text/event-stream")
        .post(RequestBody.create(requestBody.toJSONString(),
               MediaType.parse("application/json")))
        .build();

    EventSource.Factory factory = EventSources.createFactory(httpClient);
    factory.newEventSource(request, new EventSourceListener() {
        private StringBuilder fullResponse = new StringBuilder();

        @Override
        public void onEvent(EventSource es, @Nullable String type,
                           String id, @Nullable String data) {
            if ("[DONE]".equals(data)) {
                callback.onComplete(fullResponse.toString());
                return;
            }
            String chunk = parseStreamChunk(data);
            if (chunk != null && !chunk.isEmpty()) {
                fullResponse.append(chunk);
                callback.onChunk(chunk);
            }
        }

        @Override
        public void onFailure(EventSource es, @Nullable Throwable t,
                             @Nullable Response response) {
            callback.onError(t != null ? t.getMessage() : "Unknown error");
        }
    });
}

// 解析流式响应块
private String parseStreamChunk(String data) {
    try {
        JSONObject json = JSON.parseObject(data);
        JSONArray choices = json.getJSONArray("choices");
        if (choices != null && !choices.isEmpty()) {
            JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
            if (delta != null) {
                return delta.getString("content");
            }
        }
    } catch (Exception e) {
        log.warn("Failed to parse stream chunk: {}", data);
    }
    return null;
}
```

**依赖**: 需要添加 OkHttp SSE 依赖（如果还没有）
```xml
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp-sse</artifactId>
    <version>4.12.0</version>
</dependency>
```

---

### 2. AIConversationService.java - 新增流式服务方法

**文件**: `src/main/java/com/englishword/service/AIConversationService.java`

**改动内容**:
```java
/**
 * 流式聊天 - 返回 Flux 用于 SSE
 */
public Flux<String> chatStream(String userId, AIChatRequest request) {
    return Flux.create(emitter -> {
        try {
            // 1. 获取或创建对话
            AIConversation conversation = getOrCreateConversation(userId, request.getConversationId());
            String conversationHistory = conversation.getMessages();

            // 2. 设置用户上下文
            UserContext.setCurrentOperationUser(userId, null);

            // 3. 收集完整响应用于保存
            StringBuilder fullResponse = new StringBuilder();

            // 4. 调用流式 API
            String systemPrompt = getSystemPrompt(request.getMode(), request.getTrainingWords());

            chatClient.chatStream(
                systemPrompt,
                request.getMessage(),
                conversationHistory,
                new ChatClient.StreamCallback() {
                    @Override
                    public void onChunk(String chunk) {
                        fullResponse.append(chunk);
                        emitter.next(chunk);
                    }

                    @Override
                    public void onComplete(String response) {
                        // 保存对话历史
                        saveConversationHistory(conversation, request.getMessage(), response);
                        UserContext.clearOperationUser();
                        emitter.complete();
                    }

                    @Override
                    public void onError(String error) {
                        UserContext.clearOperationUser();
                        emitter.error(new RuntimeException(error));
                    }
                }
            );

        } catch (Exception e) {
            log.error("Stream chat error", e);
            emitter.error(e);
        }
    }, FluxSink.OverflowStrategy.BUFFER);
}
```

---

### 3. AIController.java - 新增流式端点

**文件**: `src/main/java/com/englishword/controller/AIController.java`

**改动内容**:
```java
/**
 * 流式聊天接口
 * POST /api/ai/chat/stream
 * 返回 SSE 格式的流式数据
 */
@PostMapping(value = "/ai/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<String>> chatStream(
        @RequestBody AIChatRequest request) {

    String userId = UserContext.getCurrentUserId();
    if (userId == null) {
        return Flux.just(ServerSentEvent.<String>builder()
            .data("{\"error\":\"Unauthorized\"}")
            .build());
    }

    return aiConversationService.chatStream(userId, request)
        .map(chunk -> ServerSentEvent.<String>builder()
            .data(chunk)
            .build())
        .concatWith(Flux.just(ServerSentEvent.<String>builder()
            .data("[DONE]")
            .build()));
}
```

---

## Android 端改动

### 1. 新建 SSEClient.kt

**文件**: `android-word-app/app/src/main/java/com/englishword/data/SSEClient.kt`

```kotlin
package com.englishword.data

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.*
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SSEClient(private val baseUrl: String) {

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    fun chatStream(endpoint: String, token: String, requestBody: String): Flow<String> = callbackFlow {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestBody.toRequestBody(mediaType)

        val request = Request.Builder()
            .url("$baseUrl$endpoint")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "text/event-stream")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        var eventSource: EventSource? = null

        try {
            val factory = EventSources.createFactory(client)
            eventSource = factory.newEventSource(request, object : EventSourceListener() {
                override fun onEvent(
                    source: EventSource,
                    type: String?,
                    id: String?,
                    data: String
                ) {
                    if (data == "[DONE]") {
                        close()
                        return
                    }
                    // 直接发送 chunk（后端已经解析好了）
                    trySend(data).isSuccess
                }

                override fun onClosed(source: EventSource) {
                    close()
                }

                override fun onFailure(source: EventSource, t: Throwable?, response: Response?) {
                    close()
                }
            })
        } catch (e: Exception) {
            close(e)
        }

        awaitClose {
            eventSource?.cancel()
        }
    }
}
```

**依赖**: 需要在 `app/build.gradle.kts` 添加：
```kotlin
implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")
```

---

### 2. 改造 AIChatViewModel.kt

**文件**: `android-word-app/app/src/main/java/com/englishword/ui/screens/AIChatViewModel.kt`

**改动内容**:

```kotlin
class AIChatViewModel : ViewModel() {
    // ... 现有代码 ...

    private val sseClient = SSEClient(RetrofitClient.BASE_URL)

    /**
     * 发送消息（流式输出）
     */
    fun sendMessage(
        message: String,
        mode: String = "word_inquiry",
        targetWord: String? = null
    ) {
        if (message.isBlank()) return

        viewModelScope.launch {
            // 1. 立即添加用户消息
            val userMsg = ChatMessage().apply {
                role = "user"
                content = message
            }
            _messages.value = _messages.value + userMsg

            // 2. 添加空的 AI 消息占位
            val aiMsgIndex = _messages.value.size
            val aiMsg = ChatMessage().apply {
                role = "assistant"
                content = ""
            }
            _messages.value = _messages.value + aiMsg
            _isLoading.value = true

            try {
                val token = RetrofitClient.getStoredToken()
                val requestBody = buildStreamRequestBody(message, mode, targetWord)
                val fullResponse = StringBuilder()

                // 3. 收集流式数据
                sseClient.chatStream("/api/ai/chat/stream", token, requestBody)
                    .collect { chunk ->
                        fullResponse.append(chunk)
                        // 实时更新 AI 消息
                        updateMessage(aiMsgIndex, fullResponse.toString())
                    }

                // 4. 更新 conversationId（从最后一条消息获取）
                // 流式完成后可以调用一个轻量接口获取 conversationId

            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
                // 更新为错误消息
                updateMessage(aiMsgIndex, "Sorry, network error occurred.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun updateMessage(index: Int, content: String) {
        val currentList = _messages.value.toMutableList()
        if (index < currentList.size) {
            currentList[index] = ChatMessage().apply {
                role = "assistant"
                this.content = content
                this.conversationId = _conversationId.value
            }
            _messages.value = currentList
        }
    }

    private fun buildStreamRequestBody(message: String, mode: String, targetWord: String?): String {
        val json = JSONObject()
        json.put("message", message)
        json.put("mode", mode)
        _conversationId.value?.let { json.put("conversationId", it) }
        targetWord?.let { json.put("targetWord", it) }
        if (mode == "word_training") {
            json.put("trainingWords", org.json.JSONArray(trainingWordsList))
        }
        return json.toString()
    }
}
```

---

### 3. 微调 AIChatScreen.kt

**文件**: `android-word-app/app/src/main/java/com/englishword/ui/screens/AIChatScreen.kt`

**改动**: 流式输出时隐藏 loading 指示器（因为已经有文字在显示）

```kotlin
// 在 LazyColumn 的 loading 指示器部分改为：
if (isLoading && messages.lastOrNull()?.content.isNullOrBlank()) {
    // 只有最后一条消息为空时才显示 loading
    item {
        // ... loading UI ...
    }
}
```

---

## 测试计划

### 1. 后端测试

```bash
# 使用 curl 测试流式接口
curl -N -X POST "http://localhost:8885/api/ai/chat/stream" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"message":"Hello","mode":"word_inquiry"}'
```

### 2. Android 测试

1. 发送消息，观察是否逐字显示
2. 测试网络中断后的恢复
3. 测试长回复（1000字以上）的流式效果

---

## 部署步骤

1. **后端部署**
   ```bash
   # 编译
   mvn clean package -DskipTests

   # 上传
   scp target/english-word-backend-1.0.0.jar ubuntu@119.91.206.195:~/projects/english-word-app/target/

   # 重启
   ssh ubuntu@119.91.206.195 "cd ~/projects/english-word-app && docker compose restart app"
   ```

2. **Android 部署**
   ```bash
   cd android-word-app
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 文件改动清单

| 文件 | 改动类型 | 预计行数 |
|------|---------|---------|
| **后端** | | |
| `pom.xml` | 新增依赖 | 5 |
| `OpenAICompatibleClient.java` | 新增方法 | 60 |
| `AIConversationService.java` | 新增方法 | 40 |
| `AIController.java` | 新增端点 | 20 |
| `ChatClient.java` | 新增接口 | 10 |
| **Android** | | |
| `build.gradle.kts` | 新增依赖 | 1 |
| `SSEClient.kt` | 新建 | 60 |
| `AIChatViewModel.kt` | 改造 | 40 |
| `AIChatScreen.kt` | 微调 | 5 |

**总计**: 约 240 行代码
