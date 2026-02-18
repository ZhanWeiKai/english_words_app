# Word Search Mode Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add word search mode to AI Chat - user inputs Chinese/English, AI returns structured word list with add-to-vault buttons.

**Architecture:** Backend adds `word_search` mode with structured JSON response. Android renders WordResultCard components with add buttons. Minimal changes to existing flows.

**Tech Stack:** Spring Boot 3.x (Java), Kotlin/Compose (Android), Retrofit, ZhipuAI API

---

## Existing Flow (DO NOT CHANGE)

```
Vault Page → FAB AI Chat Button → AIChatScreen
    ↓
User types message → sendMessage(mode=word_inquiry)
    ↓
Backend AIConversationService.chat() → ZhipuAIService.explainWord()
    ↓
Returns AIChatResponse with message (Markdown string)
    ↓
Android ChatMessageItem displays text
```

**Existing Modes:**
- `word_inquiry`: AI explains a word (DO NOT MODIFY)
- `word_training`: AI practices with user (DO NOT MODIFY)

---

## Extension Point

### Where to Insert New Feature

1. **Backend `AIConversationService.java:76-96`** - Add `word_search` mode handling
2. **Backend `ZhipuAIService.java`** - Add `searchWords()` method
3. **Backend `AIChatResponse.java`** - Add `wordResults` field
4. **Android `Models.kt:102-112`** - Add `wordResults` to AIChatResponse
5. **Android `AIChatScreen.kt`** - Add WordResultCard rendering
6. **Android `AIChatViewModel.kt`** - Add addWord() function

---

## Non-breaking Rules (CRITICAL)

1. **DO NOT modify** `word_inquiry` mode behavior
2. **DO NOT modify** `word_training` mode behavior
3. **DO NOT change** existing API contracts for other modes
4. **DO NOT change** ChatMessageItem for regular messages
5. **DO NOT modify** existing message flow or state management
6. **Backward compatible** - `wordResults` is optional in response

---

## Risk Points

| Risk | Mitigation |
|------|------------|
| AI returns malformed JSON | Add fallback to plain text, log errors |
| Word already in vault | Check before add, show "已添加" state |
| Add API fails | Toast error, keep button clickable |
| Performance with multiple words | Each word card is independent |

---

## Tasks

### Task 1: Backend - Add WordResult Data Structure

**Files:**
- Create: `english-word-backend/src/main/java/com/englishword/dto/response/WordResult.java`
- Modify: `english-word-backend/src/main/java/com/englishword/dto/response/AIChatResponse.java:35`

**Step 1: Create WordResult.java**

```java
package com.englishword.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "搜索单词结果")
public class WordResult {

    @Schema(description = "单词拼写", example = "ephemeral")
    private String word;

    @Schema(description = "音标", example = "/ɪˈfem(ə)rəl/")
    private String phonetic;

    @Schema(description = "词性", example = "adj.")
    private String partOfSpeech;

    @Schema(description = "中文释义", example = "短暂的；转瞬即逝的")
    private String meaning;

    @Schema(description = "例句", example = "Fame is ephemeral.")
    private String example;
}
```

**Step 2: Add wordResults field to AIChatResponse.java**

Add after line 22 (`private List<Suggestion> suggestions;`):

```java
    @Schema(description = "搜索单词结果列表（word_search模式）")
    private List<WordResult> wordResults;
```

**Step 3: Verify compilation**

Run:
```bash
cd english-word-backend
mvn compile -q
```
Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add english-word-backend/src/main/java/com/englishword/dto/response/WordResult.java
git add english-word-backend/src/main/java/com/englishword/dto/response/AIChatResponse.java
git commit -m "feat(backend): add WordResult data structure for word search mode"
```

---

### Task 2: Backend - Add searchWords() to ZhipuAIService

**Files:**
- Modify: `english-word-backend/src/main/java/com/englishword/service/ZhipuAIService.java`

**Step 1: Add searchWords() method**

Add after `explainWord()` method (around line 62):

```java
    /**
     * Word Search模式：搜索单词
     * 返回结构化的单词列表，便于前端解析和展示
     *
     * @param input 用户输入（中文或英文）
     * @param conversationHistory 对话历史（可选）
     * @return AI回复（包含JSON格式的单词列表）
     */
    public String searchWords(String input, String conversationHistory) {
        String systemPrompt = buildWordSearchPrompt();
        String userMessage = String.format("请搜索单词：%s", input);

        return callZhipuAI(systemPrompt, userMessage, conversationHistory);
    }
```

**Step 2: Add buildWordSearchPrompt() method**

Add after `buildWordInquiryPrompt()` method (around line 226):

```java
    /**
     * 构建Word Search模式的系统提示词
     */
    private String buildWordSearchPrompt() {
        return """
            你是"English Word App"的英语学习助手。用户会输入中文或英文，你需要返回1-3个相关单词。

            ## 输出格式要求（必须严格遵守）

            你必须返回以下JSON格式，不要包含其他文字：

            ```json
            {
              "words": [
                {
                  "word": "ephemeral",
                  "phonetic": "/ɪˈfem(ə)rəl/",
                  "partOfSpeech": "adj.",
                  "meaning": "短暂的；转瞬即逝的",
                  "example": "Fame is ephemeral."
                },
                {
                  "word": "transient",
                  "phonetic": "/ˈtrænziənt/",
                  "partOfSpeech": "adj.",
                  "meaning": "短暂的；临时的",
                  "example": "Transient workers are common in this industry."
                }
              ]
            }
            ```

            ## 规则
            1. 返回1-3个最相关的单词
            2. 如果用户输入中文，返回对应的英文单词
            3. 如果用户输入英文，返回该单词及同义词
            4. 每个单词必须包含：word, phonetic, partOfSpeech, meaning, example
            5. 例句要简洁实用（不超过15个单词）
            6. 只返回JSON，不要有其他解释文字
            7. 如果找不到相关单词，返回：{"words": []}
            """;
    }
```

**Step 3: Verify compilation**

Run:
```bash
cd english-word-backend
mvn compile -q
```
Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add english-word-backend/src/main/java/com/englishword/service/ZhipuAIService.java
git commit -m "feat(backend): add searchWords() method with structured JSON prompt"
```

---

### Task 3: Backend - Handle word_search mode in AIConversationService

**Files:**
- Modify: `english-word-backend/src/main/java/com/englishword/service/AIConversationService.java`

**Step 1: Add word_search mode handling**

Modify lines 76-96, add new case:

```java
            // 2. 根据模式调用AI
            String aiReply;
            String mode = request.getMode() != null ? request.getMode() : "word_inquiry";
            log.info("=== AI Chat Request === Mode: {}, TrainingWords: {}, TargetWord: {}",
                    mode, request.getTrainingWords(), request.getTargetWord());

            if ("word_training".equals(mode)) {
                // 训练模式
                log.info(">>> Entering TRAINING mode - calling practiceInScenario()");
                aiReply = zhipuAIService.practiceInScenario(
                        request.getTargetWord(),
                        request.getScenario(),
                        request.getTrainingWords(),
                        request.getMessage(),
                        conversationHistory
                );
            } else if ("word_search".equals(mode)) {
                // 搜索单词模式
                log.info(">>> Entering WORD_SEARCH mode - calling searchWords()");
                aiReply = zhipuAIService.searchWords(
                        request.getMessage(),
                        conversationHistory
                );
            } else {
                // 询问模式（默认）
                aiReply = zhipuAIService.explainWord(
                        request.getMessage(),
                        conversationHistory
                );
            }
```

**Step 2: Add WordResult parsing and response building**

Add after the AI call section (around line 96), modify response building:

```java
            // 4. 构建响应
            AIChatResponse response = new AIChatResponse();
            response.setConversationId(conversationId);
            response.setMessage(aiReply);

            // 如果是word_search模式，尝试解析JSON并设置wordResults
            if ("word_search".equals(mode)) {
                try {
                    List<AIChatResponse.WordResult> wordResults = parseWordResults(aiReply);
                    if (wordResults != null && !wordResults.isEmpty()) {
                        response.setWordResults(wordResults);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse word search results: {}", e.getMessage());
                    // 继续使用原始message
                }
            }

            // 生成建议操作
            List<AIChatResponse.Suggestion> suggestions = generateSuggestions(mode, request.getTargetWord());
            response.setSuggestions(suggestions);
```

**Step 3: Add parseWordResults() helper method**

Add at the end of the class (before the closing brace):

```java
    /**
     * 解析AI返回的单词搜索结果
     */
    private List<AIChatResponse.WordResult> parseWordResults(String aiReply) {
        try {
            // 提取JSON部分（可能被markdown代码块包裹）
            String jsonContent = aiReply;

            // 如果包含```json，提取其中的内容
            if (aiReply.contains("```json")) {
                int start = aiReply.indexOf("```json") + 7;
                int end = aiReply.indexOf("```", start);
                if (end > start) {
                    jsonContent = aiReply.substring(start, end).trim();
                }
            } else if (aiReply.contains("```")) {
                int start = aiReply.indexOf("```") + 3;
                int end = aiReply.indexOf("```", start);
                if (end > start) {
                    jsonContent = aiReply.substring(start, end).trim();
                }
            }

            // 解析JSON
            JSONObject jsonResponse = JSON.parseObject(jsonContent);
            JSONArray wordsArray = jsonResponse.getJSONArray("words");

            if (wordsArray == null || wordsArray.isEmpty()) {
                return null;
            }

            List<AIChatResponse.WordResult> results = new ArrayList<>();
            for (int i = 0; i < wordsArray.size(); i++) {
                JSONObject wordObj = wordsArray.getJSONObject(i);
                AIChatResponse.WordResult result = new AIChatResponse.WordResult();
                result.setWord(wordObj.getString("word"));
                result.setPhonetic(wordObj.getString("phonetic"));
                result.setPartOfSpeech(wordObj.getString("partOfSpeech"));
                result.setMeaning(wordObj.getString("meaning"));
                result.setExample(wordObj.getString("example"));
                results.add(result);
            }

            return results;

        } catch (Exception e) {
            log.error("Failed to parse word results JSON: {}", e.getMessage());
            return null;
        }
    }
```

**Step 4: Add import for WordResult**

Add at top of file if needed:
```java
import com.englishword.dto.response.AIChatResponse.WordResult;
```

**Step 5: Verify compilation**

Run:
```bash
cd english-word-backend
mvn compile -q
```
Expected: BUILD SUCCESS

**Step 6: Commit**

```bash
git add english-word-backend/src/main/java/com/englishword/service/AIConversationService.java
git commit -m "feat(backend): handle word_search mode with JSON parsing"
```

---

### Task 4: Backend - Build and Test Locally

**Step 1: Build JAR**

Run:
```bash
cd english-word-backend
mvn clean package -DskipTests
```
Expected: BUILD SUCCESS, JAR at `target/english-word-backend-1.0.0.jar`

**Step 2: Run local unit tests**

Run:
```bash
cd english-word-backend
mvn test
```
Expected: All tests pass

**Step 3: Commit if any fixes needed**

```bash
git add -A
git commit -m "fix(backend): test fixes for word_search mode"
```

---

### Task 5: Backend - Deploy and Remote Test

**Step 1: Upload JAR to server**

Run:
```bash
scp english-word-backend/target/english-word-backend-1.0.0.jar root@47.83.126.42:/root/english-word-app/target/
```
Expected: File uploaded successfully

**Step 2: Restart Docker container**

Run:
```bash
ssh root@47.83.126.42 "cd /root/english-word-app && docker compose restart app"
```
Expected: Container restarted

**Step 3: Test word_search API**

Run:
```bash
curl -X POST 'http://47.83.126.42:8885/api/ai/chat' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer YOUR_TOKEN' \
  -d '{"message": "短暂", "mode": "word_search"}'
```

Expected response:
```json
{
  "code": 200,
  "data": {
    "conversationId": "...",
    "message": "...",
    "wordResults": [
      {
        "word": "ephemeral",
        "phonetic": "/ɪˈfem(ə)rəl/",
        "partOfSpeech": "adj.",
        "meaning": "短暂的",
        "example": "Fame is ephemeral."
      }
    ]
  }
}
```

**Step 4: Check server logs if fails**

Run:
```bash
ssh root@47.83.126.42 "docker compose -f /root/english-word-app/docker-compose.yml logs --tail=50 app"
```

**Step 5: Verify and commit**

```bash
git add -A
git commit -m "feat(backend): word_search mode deployed and tested"
```

---

### Task 6: Android - Add WordResult model

**Files:**
- Modify: `android-word-app/app/src/main/java/com/englishword/data/model/Models.kt`

**Step 1: Add WordResult data class**

Add after `AIChatRequest` class (around line 124):

```kotlin
/**
 * Word search result from AI
 */
data class WordResult(
    var word: String? = null,
    var phonetic: String? = null,
    var partOfSpeech: String? = null,
    var meaning: String? = null,
    var example: String? = null
)
```

**Step 2: Add wordResults to AIChatResponse**

Modify `AIChatResponse` class (lines 102-112):

```kotlin
/**
 * AI Chat response from backend
 */
data class AIChatResponse(
    var conversationId: String? = null,
    var message: String? = null,
    var suggestions: List<Suggestion>? = null,
    var wordResults: List<WordResult>? = null  // For word_search mode
) {
    data class Suggestion(
        var type: String? = null,
        var word: String? = null,
        var label: String? = null
    )
}
```

**Step 3: Verify compilation**

Run:
```bash
cd android-word-app
./gradlew compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add android-word-app/app/src/main/java/com/englishword/data/model/Models.kt
git commit -m "feat(android): add WordResult model for word search"
```

---

### Task 7: Android - Add WordResultCard Component

**Files:**
- Create: `android-word-app/app/src/main/java/com/englishword/ui/components/WordResultCard.kt`

**Step 1: Create WordResultCard.kt**

```kotlin
package com.englishword.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.englishword.data.model.WordResult

@Composable
fun WordResultCard(
    wordResult: WordResult,
    isAdded: Boolean = false,
    isAdding: Boolean = false,
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Word and phonetic
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = wordResult.word ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = wordResult.phonetic ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Part of speech and meaning
            Text(
                text = "${wordResult.partOfSpeech} ${wordResult.meaning}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Example
            Text(
                text = "例：${wordResult.example}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Add button
            Button(
                onClick = onAddClick,
                enabled = !isAdded && !isAdding,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAdded)
                        MaterialTheme.colorScheme.outline
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                if (isAdding) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("添加中...")
                } else {
                    Text(if (isAdded) "已添加" else "添加")
                }
            }
        }
    }
}
```

**Step 2: Verify compilation**

Run:
```bash
cd android-word-app
./gradlew compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add android-word-app/app/src/main/java/com/englishword/ui/components/WordResultCard.kt
git commit -m "feat(android): add WordResultCard component with add button"
```

---

### Task 8: Android - Update AIChatViewModel with addWord logic

**Files:**
- Modify: `android-word-app/app/src/main/java/com/englishword/ui/screens/AIChatViewModel.kt`

**Step 1: Add state for tracking added words**

Add after line 28 (`private val _error = MutableStateFlow<String?>(null)`):

```kotlin
    // Track added words in current session
    private val _addedWords = MutableStateFlow<Set<String>>(emptySet())
    val addedWords: StateFlow<Set<String>> = _addedWords.asStateFlow()

    // Track words currently being added
    private val _addingWords = MutableStateFlow<Set<String>>(emptySet())
    val addingWords: StateFlow<Set<String>> = _addingWords.asStateFlow()
```

**Step 2: Add addWord() function**

Add after `sendMessage()` function (around line 164):

```kotlin
    /**
     * Add word to user's vault
     */
    fun addWord(wordResult: com.englishword.data.model.WordResult) {
        val wordText = wordResult.word ?: return

        // Prevent duplicate adds
        if (_addedWords.value.contains(wordText) || _addingWords.value.contains(wordText)) {
            return
        }

        viewModelScope.launch {
            // Mark as adding
            _addingWords.value = _addingWords.value + wordText

            try {
                val word = com.englishword.data.model.Word().apply {
                    this.word = wordResult.word
                    this.phonetic = wordResult.phonetic
                    this.partOfSpeech = wordResult.partOfSpeech
                    this.definition = wordResult.example
                    this.translation = wordResult.meaning
                    this.status = "LEARNING"
                }

                val response = apiService.addWord(word).execute()

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    // Mark as added
                    _addedWords.value = _addedWords.value + wordText
                } else {
                    _error.value = "添加失败"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "网络错误"
            } finally {
                // Remove from adding set
                _addingWords.value = _addingWords.value - wordText
            }
        }
    }
```

**Step 3: Add search mode function**

Add after `addWord()` function:

```kotlin
    /**
     * Send message in word search mode
     */
    fun searchWords(message: String) {
        if (message.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // Add user message immediately
            val userMsg = ChatMessage().apply {
                role = "user"
                content = message
            }
            _messages.value = _messages.value + userMsg

            try {
                val request = AIChatRequest().apply {
                    this.message = message
                    this.mode = "word_search"
                }

                val response = apiService.chat(request)

                if (response.isSuccess && response.data != null) {
                    val chatResponse = response.data!!
                    _conversationId.value = chatResponse.conversationId

                    // Add AI response with word results
                    val aiMsg = ChatMessage().apply {
                        role = "assistant"
                        content = chatResponse.message
                        conversationId = chatResponse.conversationId
                    }
                    _messages.value = _messages.value + aiMsg

                    // Store word results for rendering
                    // This will be handled in the Screen
                } else {
                    _error.value = response.message ?: "搜索失败"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "网络错误"
            } finally {
                _isLoading.value = false
            }
        }
    }
```

**Step 4: Update ChatMessage to include wordResults**

Modify Models.kt ChatMessage class (around line 85-97):

```kotlin
/**
 * Chat message model for AI chat
 */
data class ChatMessage(
    var id: String? = null,
    var conversationId: String? = null,
    var role: String? = null, // "user" or "assistant"
    var content: String? = null,
    var createdAt: String? = null,
    var wordResults: List<WordResult>? = null  // For word_search mode
) {
    val isUser: Boolean
        get() = role == "user"

    val isAssistant: Boolean
        get() = role == "assistant"

    val hasWordResults: Boolean
        get() = !wordResults.isNullOrEmpty()
}
```

**Step 5: Update sendMessage to pass wordResults**

In `sendMessage()` function, modify the aiMsg creation (around line 137):

```kotlin
                    // Add AI response
                    val aiMsg = ChatMessage().apply {
                        role = "assistant"
                        content = chatResponse.message
                        conversationId = chatResponse.conversationId
                        wordResults = chatResponse.wordResults  // Pass word results
                    }
                    _messages.value = _messages.value + aiMsg
```

**Step 6: Verify compilation**

Run:
```bash
cd android-word-app
./gradlew compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

**Step 7: Commit**

```bash
git add android-word-app/app/src/main/java/com/englishword/ui/screens/AIChatViewModel.kt
git add android-word-app/app/src/main/java/com/englishword/data/model/Models.kt
git commit -m "feat(android): add addWord() and searchWords() to ViewModel"
```

---

### Task 9: Android - Update AIChatScreen to render WordResultCards

**Files:**
- Modify: `android-word-app/app/src/main/java/com/englishword/ui/screens/AIChatScreen.kt`

**Step 1: Add import for WordResultCard**

Add at top of file:
```kotlin
import com.englishword.ui.components.WordResultCard
```

**Step 2: Add collectAsState for addedWords and addingWords**

Add after line 61 (`val isLoading by viewModel.isLoading.collectAsState()`):

```kotlin
    val addedWords by viewModel.addedWords.collectAsState()
    val addingWords by viewModel.addingWords.collectAsState()
```

**Step 3: Modify message rendering to handle wordResults**

Replace the `items(messages)` block (around lines 150-152):

```kotlin
                items(messages) { message ->
                    if (message.hasWordResults) {
                        // Render word search results
                        WordSearchResultMessage(
                            message = message,
                            addedWords = addedWords,
                            addingWords = addingWords,
                            onAddWord = { wordResult ->
                                viewModel.addWord(wordResult)
                            }
                        )
                    } else {
                        // Regular message
                        ChatMessageItem(message = message)
                    }
                }
```

**Step 4: Add WordSearchResultMessage composable**

Add at the end of the file (before the last closing brace):

```kotlin
@Composable
fun WordSearchResultMessage(
    message: ChatMessage,
    addedWords: Set<String>,
    addingWords: Set<String>,
    onAddWord: (WordResult) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        // Header text
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.clip(MaterialTheme.shapes.medium)
        ) {
            Text(
                text = "找到相关单词：",
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Word cards
        message.wordResults?.forEach { wordResult ->
            WordResultCard(
                wordResult = wordResult,
                isAdded = addedWords.contains(wordResult.word),
                isAdding = addingWords.contains(wordResult.word),
                onAddClick = { onAddWord(wordResult) }
            )
        }
    }
}
```

**Step 5: Verify compilation**

Run:
```bash
cd android-word-app
./gradlew compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add android-word-app/app/src/main/java/com/englishword/ui/screens/AIChatScreen.kt
git commit -m "feat(android): render WordResultCards in AIChatScreen"
```

---

### Task 10: Android - Build and Test on Device

**Step 1: Build debug APK**

Run:
```bash
cd android-word-app
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL, APK at `app/build/outputs/apk/debug/app-debug.apk`

**Step 2: Install on device**

Run:
```bash
adb install -r android-word-app/app/build/outputs/apk/debug/app-debug.apk
```
Expected: Success

**Step 3: Test on device**

1. Open app
2. Login
3. Go to AI Chat
4. Enter a Chinese word (e.g., "短暂")
5. Verify AI returns word cards
6. Click "添加" button
7. Verify button changes to "已添加"

**Step 4: Check Logcat if issues**

Run:
```bash
adb logcat | grep -E "EnglishWord|AIChat"
```

**Step 5: Commit any fixes**

```bash
git add -A
git commit -m "fix(android): test fixes for word search mode"
```

---

### Task 11: Validation Gate - Full Test Suite

**Step 1: Run all backend tests**

Run:
```bash
cd english-word-backend
mvn test
```
Expected: All tests pass

**Step 2: Run all Android tests**

Run:
```bash
cd android-word-app
./gradlew test
```
Expected: All tests pass

**Step 3: Remote API test**

Run:
```bash
curl -X POST 'http://47.83.126.42:8885/api/ai/chat' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer YOUR_TOKEN' \
  -d '{"message": "快乐", "mode": "word_search"}'
```
Expected: Returns wordResults with happy-related words

**Step 4: Device test**

- Test word_inquiry mode still works (existing functionality)
- Test word_training mode still works (existing functionality)
- Test word_search mode with various inputs

**Step 5: Final commit**

```bash
git add -A
git commit -m "feat: add word search mode with add-to-vault functionality

- Backend: add word_search mode with structured JSON response
- Backend: add WordResult data structure
- Android: add WordResultCard component
- Android: add addWord() logic in ViewModel
- All existing modes (word_inquiry, word_training) unchanged"
```

---

## Summary

| Task | Description | Platform |
|------|-------------|----------|
| 1 | Add WordResult data structure | Backend |
| 2 | Add searchWords() to ZhipuAIService | Backend |
| 3 | Handle word_search mode | Backend |
| 4 | Build and test locally | Backend |
| 5 | Deploy and remote test | Backend |
| 6 | Add WordResult model | Android |
| 7 | Create WordResultCard component | Android |
| 8 | Update ViewModel with addWord() | Android |
| 9 | Update AIChatScreen rendering | Android |
| 10 | Build and test on device | Android |
| 11 | Validation gate | Both |
