# MCP Add Word Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add MCP server to backend that exposes `add_word` tool for Android app to add words via AI chat.

**Architecture:** MCP Server runs on Spring Boot backend at `/api/mcp`, uses SSE protocol, exposes one tool `add_word` that calls Zhipu AI to fill word details then saves to database.

**Tech Stack:** Spring Boot 3.2, MCP Server SDK (webmvc-sse), Zhipu AI API, existing JWT auth

---

## File Structure

```
english-word-backend/src/main/java/com/englishword/
├── mcp/
│   ├── McpServerConfig.java        # MCP Server 配置
│   ├── McpToolsService.java        # add_word 工具实现
│   └── WordInfoGenerator.java      # 调用智谱AI生成单词信息
└── pom.xml                         # 添加 MCP 依赖
```

---

## Chunk 1: Backend MCP Dependencies and Config

### Task 1: Add MCP Dependencies

**Files:**
- Modify: `english-word-backend/pom.xml`

- [ ] **Step 1: Add MCP Server dependency to pom.xml**

Add after line 111 (after okhttp dependency):

```xml
<!-- MCP Server for Spring Boot WebMVC -->
<dependency>
    <groupId>io.modelcontextprotocol</groupId>
    <artifactId>mcp-server-spring-webmvc</artifactId>
    <version>0.5.0</version>
</dependency>
```

- [ ] **Step 2: Reload Maven dependencies**

Run:
```bash
cd C:\claude-project\english-word-app\english-word-backend && "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.5\plugins\maven\lib\maven3\bin\mvn.cmd" dependency:resolve -q
```

Expected: BUILD SUCCESS

---

### Task 2: Create MCP Server Configuration

**Files:**
- Create: `english-word-backend/src/main/java/com/englishword/mcp/McpServerConfig.java`

- [ ] **Step 1: Create MCP Server Config class**

```java
package com.englishword.mcp;

import com.englishword.mcp.McpToolsService;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.WebMvcSseServerTransport;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * MCP Server 配置
 *
 * 暴露工具供 Android App 调用
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class McpServerConfig {

    private final McpToolsService mcpToolsService;

    @Bean
    public WebMvcSseServerTransport webMvcSseServerTransport() {
        return new WebMvcSseServerTransport("/mcp");
    }

    @Bean
    public McpSyncServer mcpServer(WebMvcSseServerTransport transport) {
        McpSyncServer server = McpServer.sync(transport)
                .serverInfo("english-word-mcp", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .build();

        // 注册 add_word 工具
        registerAddWordTool(server);

        log.info("MCP Server initialized with add_word tool");
        return server;
    }

    private void registerAddWordTool(McpSyncServer server) {
        McpSchema.Tool tool = new McpSchema.Tool(
                "add_word",
                "添加单词到用户单词库。自动调用AI填充详细信息（音标、词性、释义、例句等）。参数：word(必需)-英文单词或词组，translation(可选)-中文翻译",
                new McpSchema.JsonSchema(
                        "object",
                        List.of("word"),
                        java.util.Map.of(
                                "word", java.util.Map.of("type", "string", "description", "要添加的英文单词或词组"),
                                "translation", java.util.Map.of("type", "string", "description", "中文翻译（可选）")
                        ),
                        null
                )
        );

        server.addTool(tool, (args) -> mcpToolsService.addWord(args));
    }
}
```

- [ ] **Step 2: Create placeholder McpToolsService**

Create file: `english-word-backend/src/main/java/com/englishword/mcp/McpToolsService.java`

```java
package com.englishword.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * MCP 工具实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpToolsService {

    public McpSchema.CallToolResult addWord(Map<String, Object> args) {
        String word = (String) args.get("word");
        String translation = (String) args.get("translation");

        log.info("MCP add_word called: word={}, translation={}", word, translation);

        // TODO: 实现添加单词逻辑
        return new McpSchema.CallToolResult(
                java.util.List.of(new McpSchema.TextContent("Word added: " + word)),
                false
        );
    }
}
```

- [ ] **Step 3: Verify compilation**

Run:
```bash
cd C:\claude-project\english-word-app\english-word-backend && "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.5\plugins\maven\lib\maven3\bin\mvn.cmd" compile -q
```

Expected: BUILD SUCCESS

---

## Chunk 2: Word Info Generator Service

### Task 3: Create WordInfoGenerator Service

**Files:**
- Create: `english-word-backend/src/main/java/com/englishword/mcp/WordInfoGenerator.java`

- [ ] **Step 1: Create WordInfoGenerator class**

```java
package com.englishword.mcp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.englishword.entity.Word;
import com.englishword.service.ZhipuAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 使用智谱AI生成单词详细信息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WordInfoGenerator {

    private final ZhipuAIService zhipuAIService;

    /**
     * 为单词生成完整信息
     *
     * @param word 单词/词组
     * @param translation 中文翻译（可选）
     * @return 完整的Word对象（不含userId和wordId）
     */
    public Word generateWordInfo(String word, String translation) {
        String prompt = buildPrompt(word, translation);
        String aiResponse = zhipuAIService.chat(prompt, null, getSystemPrompt());

        log.info("AI response for word '{}': {}", word, aiResponse);

        return parseAiResponse(word, translation, aiResponse);
    }

    private String getSystemPrompt() {
        return """
            你是一个英语单词信息生成助手。用户会给你一个英文单词或词组，你需要返回该单词的详细信息。

            你必须严格按以下JSON格式返回，不要包含其他文字：

            {
              "pronunciation": "/音标/",
              "partOfSpeech": "词性（如 n./v./adj./adv./phrase 等）",
              "definition": "英文释义",
              "translation": "中文翻译",
              "exampleSentence": "英文例句",
              "exampleTranslation": "例句中文翻译"
            }

            规则：
            1. 如果用户提供translation，使用用户提供的翻译
            2. 音标使用IPA格式
            3. 例句要实用且不超过20个单词
            4. 只返回JSON，不要有其他文字
            """;
    }

    private String buildPrompt(String word, String translation) {
        if (translation != null && !translation.isEmpty()) {
            return String.format("单词：%s，中文翻译：%s", word, translation);
        }
        return String.format("单词：%s", word);
    }

    private Word parseAiResponse(String word, String userTranslation, String aiResponse) {
        Word wordEntity = new Word();
        wordEntity.setWord(word);
        wordEntity.setStatus("LEARNING");
        wordEntity.setMasteryLevel(1);

        try {
            // 提取JSON部分（处理可能的markdown代码块）
            String jsonStr = extractJson(aiResponse);
            JSONObject json = JSON.parseObject(jsonStr);

            wordEntity.setPronunciation(json.getString("pronunciation"));
            wordEntity.setPartOfSpeech(json.getString("partOfSpeech"));
            wordEntity.setDefinition(json.getString("definition"));
            // 优先使用用户提供的翻译
            wordEntity.setTranslation(userTranslation != null && !userTranslation.isEmpty()
                    ? userTranslation : json.getString("translation"));
            wordEntity.setExampleSentence(json.getString("exampleSentence"));
            wordEntity.setExampleTranslation(json.getString("exampleTranslation"));

        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", aiResponse, e);
            // 使用默认值
            wordEntity.setTranslation(userTranslation != null ? userTranslation : "请手动添加翻译");
            wordEntity.setPronunciation("");
            wordEntity.setPartOfSpeech("");
            wordEntity.setDefinition("");
            wordEntity.setExampleSentence("");
            wordEntity.setExampleTranslation("");
        }

        return wordEntity;
    }

    private String extractJson(String response) {
        // 处理markdown代码块
        if (response.contains("```json")) {
            int start = response.indexOf("```json") + 7;
            int end = response.indexOf("```", start);
            return response.substring(start, end).trim();
        }
        if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.indexOf("```", start);
            return response.substring(start, end).trim();
        }
        // 尝试直接解析
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}") + 1;
        if (start >= 0 && end > start) {
            return response.substring(start, end);
        }
        return response;
    }
}
```

- [ ] **Step 2: Update Word entity to add 'word' field**

Check if Word entity has a `word` field (the actual word string). Looking at the entity, it doesn't seem to have it. Add it.

Read the Word entity first to confirm, then add the missing field.

**Modify:** `english-word-backend/src/main/java/com/englishword/entity/Word.java`

Add after line 27 (after userId field):

```java
    /**
     * 单词/词组
     */
    @Column(nullable = false, length = 255)
    private String word;
```

Wait - looking at the Word entity again, I see it has `wordId` but not a `word` field for the actual English word. Let me check again...

Actually, looking at the entity more carefully, I don't see a field for storing the actual English word text. The entity has `wordId`, `userId`, `pronunciation`, `partOfSpeech`, `definition`, `translation`, etc. but no `word` field.

Let me re-read the entity to be sure... Actually the entity file shows all fields. There's no `word` field. We need to add it.

- [ ] **Step 3: Verify compilation**

Run:
```bash
cd C:\claude-project\english-word-app\english-word-backend && "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.5\plugins\maven\lib\maven3\bin\mvn.cmd" compile -q
```

Expected: BUILD SUCCESS

---

## Chunk 3: Complete McpToolsService Implementation

### Task 4: Implement add_word Tool Logic

**Files:**
- Modify: `english-word-backend/src/main/java/com/englishword/mcp/McpToolsService.java`

- [ ] **Step 1: Update McpToolsService with full implementation**

```java
package com.englishword.mcp;

import com.alibaba.fastjson2.JSON;
import com.englishword.dto.response.ApiResponse;
import com.englishword.entity.Word;
import com.englishword.service.WordService;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;

/**
 * MCP 工具实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpToolsService {

    private final WordInfoGenerator wordInfoGenerator;
    private final WordService wordService;

    /**
     * 添加单词工具
     *
     * @param args 工具参数 {word: string, translation?: string}
     * @return 工具执行结果
     */
    public McpSchema.CallToolResult addWord(Map<String, Object> args) {
        String word = (String) args.get("word");
        String translation = (String) args.get("translation");

        log.info("MCP add_word called: word={}, translation={}", word, translation);

        // 参数验证
        if (word == null || word.trim().isEmpty()) {
            return errorResult("参数错误：word 不能为空");
        }

        word = word.trim();

        try {
            // 获取当前用户ID
            String userId = getCurrentUserId();
            if (userId == null) {
                return errorResult("未授权：无法获取用户信息");
            }

            // 生成单词详细信息
            Word wordEntity = wordInfoGenerator.generateWordInfo(word, translation);

            // 保存到数据库
            ApiResponse<Word> response = wordService.addWord(userId, wordEntity);

            if (response.getCode() == 200) {
                String successMsg = String.format(
                        "已成功添加单词【%s】到你的单词库！\n音标：%s\n词性：%s\n释义：%s\n例句：%s",
                        word,
                        wordEntity.getPronunciation(),
                        wordEntity.getPartOfSpeech(),
                        wordEntity.getTranslation(),
                        wordEntity.getExampleSentence()
                );
                return successResult(successMsg);
            } else {
                return errorResult("添加失败：" + response.getMessage());
            }

        } catch (Exception e) {
            log.error("Error adding word: {}", word, e);
            return errorResult("添加失败：" + e.getMessage());
        }
    }

    /**
     * 获取当前登录用户ID
     */
    private String getCurrentUserId() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            return (String) request.getAttribute("userId");
        }
        return null;
    }

    private McpSchema.CallToolResult successResult(String message) {
        return new McpSchema.CallToolResult(
                List.of(new McpSchema.TextContent(message)),
                false
        );
    }

    private McpSchema.CallToolResult errorResult(String message) {
        return new McpSchema.CallToolResult(
                List.of(new McpSchema.TextContent("❌ " + message)),
                true
        );
    }
}
```

- [ ] **Step 2: Verify compilation**

Run:
```bash
cd C:\claude-project\english-word-app\english-word-backend && "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.5\plugins\maven\lib\maven3\bin\mvn.cmd" compile -q
```

Expected: BUILD SUCCESS

---

## Chunk 4: MCP Endpoint and Authentication

### Task 5: Configure MCP Endpoint Authentication

**Files:**
- Modify: `english-word-backend/src/main/java/com/englishword/config/WebMvcConfig.java`

- [ ] **Step 1: Exclude MCP endpoint from JWT interceptor**

MCP uses SSE which handles auth differently. Add `/mcp/**` to excluded paths.

Modify `english-word-backend/src/main/java/com/englishword/config/WebMvcConfig.java`:

Add `/mcp/**` to the excludePathPatterns:

```java
.excludePathPatterns(
        "/auth/register",
        "/auth/login",
        "/auth/logout",
        "/error",
        "/health",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/api-docs/**",
        "/swagger-resources/**",
        "/v3/api-docs/**",
        "/static/**",
        "/favicon.ico",
        "/ws/**",
        "/mcp/**"  // MCP SSE endpoint
);
```

Wait - this would make MCP unauthenticated. But we need authentication. Let me reconsider...

Actually, for MCP SSE, we need a different approach. The MCP SDK handles the transport, but we need to pass auth through. Let me think about this...

Option 1: Use query parameter for token: `/mcp?token=xxx`
Option 2: Custom interceptor that handles SSE
Option 3: Pass userId as parameter to tool calls

For simplicity in the first implementation, let's use Option 1: token via query parameter.

Actually, let me not exclude it from auth. Instead, we need to handle SSE authentication properly. The MCP client will send the Authorization header with the initial SSE connection.

Let me keep the MCP endpoint protected and let the SSE connection handle auth.

**Actually, revert this change** - don't add `/mcp/**` to excluded paths. The MCP SSE connection should still go through JWT validation.

But wait, SSE connections work differently. The initial GET request to establish SSE will have the header, which should work with our interceptor.

Let me proceed without changes to WebMvcConfig and see if it works.

- [ ] **Step 2: Create MCP Controller for SSE endpoint**

Actually, looking at the MCP SDK, `WebMvcSseServerTransport` should handle the endpoint automatically. But we need to ensure it's registered properly.

Let me check if we need an explicit controller...

The MCP SDK's `WebMvcSseServerTransport` should automatically register the endpoint at `/mcp`. We need to ensure Spring picks it up.

Actually, we need to register the transport with Spring MVC. Let me update the config.

- [ ] **Step 3: Update McpServerConfig to register endpoint**

Update `McpServerConfig.java` to properly register the SSE endpoint:

```java
package com.englishword.mcp;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.WebMvcSseServerTransport;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * MCP Server 配置
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class McpServerConfig {

    private final McpToolsService mcpToolsService;

    @Bean
    public WebMvcSseServerTransport webMvcSseServerTransport() {
        return new WebMvcSseServerTransport("/mcp");
    }

    @Bean
    public McpSyncServer mcpServer(WebMvcSseServerTransport transport) {
        McpSyncServer server = McpServer.sync(transport)
                .serverInfo("english-word-mcp", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .build();

        // 注册 add_word 工具
        registerAddWordTool(server);

        log.info("MCP Server initialized at /api/mcp with add_word tool");
        return server;
    }

    private void registerAddWordTool(McpSyncServer server) {
        McpSchema.Tool tool = new McpSchema.Tool(
                "add_word",
                "添加单词到用户单词库。自动调用AI填充详细信息（音标、词性、释义、例句等）。当用户想要添加、记录、保存某个英语单词或词组时调用此工具。",
                new McpSchema.JsonSchema(
                        "object",
                        List.of("word"),
                        Map.of(
                                "word", Map.of("type", "string", "description", "要添加的英文单词或词组"),
                                "translation", Map.of("type", "string", "description", "中文翻译（可选）")
                        ),
                        List.of("word")
                )
        );

        server.addTool(tool, (args) -> mcpToolsService.addWord(args));
        log.info("Registered MCP tool: add_word");
    }
}
```

- [ ] **Step 4: Verify compilation**

Run:
```bash
cd C:\claude-project\english-word-app\english-word-backend && "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.5\plugins\maven\lib\maven3\bin\mvn.cmd" compile -q
```

Expected: BUILD SUCCESS

---

## Chunk 5: Testing and Deployment

### Task 6: Local Testing

- [ ] **Step 1: Start backend server**

Run:
```bash
cd C:\claude-project\english-word-app\english-word-backend && "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.5\plugins\maven\lib\maven3\bin\mvn.cmd" spring-boot:run
```

- [ ] **Step 2: Test MCP endpoint with curl**

First get a token by logging in:
```bash
curl -X POST http://localhost:8885/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"123456"}'
```

Expected: JSON with token

- [ ] **Step 3: Test MCP tools/list**

```bash
curl -N http://localhost:8885/api/mcp/sse \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Accept: text/event-stream"
```

Expected: SSE stream with server info

### Task 7: Deploy to Server

- [ ] **Step 1: Build JAR**

Run:
```bash
cd C:\claude-project\english-word-app\english-word-backend && "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.5\plugins\maven\lib\maven3\bin\mvn.cmd" clean package -DskipTests
```

Expected: BUILD SUCCESS, JAR at `target/english-word-backend-1.0.0.jar`

- [ ] **Step 2: Upload JAR to server**

```bash
scp C:\claude-project\english-word-app\english-word-backend\target\english-word-backend-1.0.0.jar root@47.83.126.42:/root/english-word-app/target/
```

- [ ] **Step 3: Restart Docker container**

```bash
ssh root@47.83.126.42 "cd /root/english-word-app && docker compose restart app"
```

- [ ] **Step 4: Check logs**

```bash
ssh root@47.83.126.42 "docker compose -f /root/english-word-app/docker-compose.yml logs --tail=100 app"
```

Expected: Logs showing "MCP Server initialized"

- [ ] **Step 5: Commit changes**

```bash
cd C:\claude-project\english-word-app && git add -A && git commit -m "feat(backend): add MCP server with add_word tool"
```

---

## Summary

| Task | Description | Files |
|------|-------------|-------|
| 1 | Add MCP dependencies | pom.xml |
| 2 | Create MCP Server config | mcp/McpServerConfig.java, mcp/McpToolsService.java |
| 3 | Create WordInfoGenerator | mcp/WordInfoGenerator.java, entity/Word.java |
| 4 | Implement add_word logic | mcp/McpToolsService.java |
| 5 | Configure auth | config/WebMvcConfig.java |
| 6 | Local testing | - |
| 7 | Deploy to server | - |
