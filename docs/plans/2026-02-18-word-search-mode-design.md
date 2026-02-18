# Word Search Mode Design Document

**Date**: 2026-02-18
**Status**: Design Complete
**Feature**: Enhance AI Chat with word search and add-to-vault functionality

---

## Overview

Add a "Search Word Mode" to the existing AI Chat feature. Users can input English or Chinese words, AI returns multiple related words with simplified definitions, and users can add words to their vault with one click.

---

## User Flow

```
Vault Page → AI Chat Tab → Input (Chinese/English) → AI Returns Words → Click Add → Word Saved
```

---

## Design Details

### 1. AI Response Format (Simplified)

When user inputs a word (e.g., "短暂"):

```
找到相关单词：

1. **ephemeral** /ɪˈfem(ə)rəl/
adj. 短暂的；转瞬即逝的
例：Fame is ephemeral. 名声是短暂的。

2. **transient** /ˈtrænziənt/
adj. 短暂的；临时的
例：Transient workers. 临时工。
```

### 2. Add Button States

| State | Appearance | Behavior |
|-------|------------|----------|
| Not Added | Blue button "添加" | Clickable |
| Adding | "添加中..." with spinner | Disabled |
| Added | Gray button "已添加" | Disabled |

### 3. Toast Messages

- Success: "已添加到单词库 ✓"
- Already exists: "该单词已在单词库中"
- Error: "添加失败，请重试"

---

## Architecture

### Files to Modify

**Backend (Java):**
- `ZhipuAIService.java` - Add `searchWords()` method with new prompt
- `AIConversationService.java` - Handle `word_search` mode
- `AIChatResponse.java` - Add `wordResults` field

**Android (Kotlin):**
- `AIChatScreen.kt` - Render word search results
- `AIChatViewModel.kt` - Handle add word logic
- New: `WordResultCard.kt` - Word result card component

### Data Flow

```
User Input (Chinese/English)
    ↓
AI Chat API (mode=word_search)
    ↓
Backend returns structured word list
    ↓
Android renders WordResultCard
    ↓
User clicks "Add" → addWord API
    ↓
Button state changes to "已添加"
```

---

## Backend Design

### 1. New AI Mode

```java
// AIConversationService.java
if ("word_search".equals(mode)) {
    aiReply = zhipuAIService.searchWords(request.getMessage());
}
```

### 2. AI Prompt Requirements

- Return 1-3 related words
- Each word includes: spelling, phonetic, part of speech, Chinese meaning, one example
- Use structured JSON format for easy parsing

### 3. Response Data Structure

```java
// AIChatResponse.java
private List<WordResult> wordResults;

public static class WordResult {
    private String word;         // 单词
    private String phonetic;     // 音标
    private String partOfSpeech; // 词性
    private String meaning;      // 中文释义
    private String example;      // 例句
}
```

### 4. Check Word Exists (Optional)

```
GET /api/words/check?word={word}
Response: { exists: true/false }
```

Or use existing `searchWords` API to check locally.

---

## Frontend Design (Android)

### 1. AIChatScreen Changes

```kotlin
// Render based on message type
if (message.wordResults != null) {
    WordSearchResultCard(
        wordResults = message.wordResults,
        existingWords = existingWords,
        onAddWord = { word -> viewModel.addWord(word) }
    )
} else {
    ChatMessageItem(message = message)
}
```

### 2. WordResultCard Component

```kotlin
@Composable
fun WordResultCard(
    word: WordResult,
    isAdded: Boolean,
    onAddClick: () -> Unit
) {
    Card {
        Column {
            Text("${word.word} /${word.phonetic}/")
            Text("${word.partOfSpeech} ${word.meaning}")
            Text("例：${word.example}")

            Button(
                onClick = onAddClick,
                enabled = !isAdded,
                colors = if (isAdded) grayButtonColors else blueButtonColors
            ) {
                Text(if (isAdded) "已添加" else "添加")
            }
        }
    }
}
```

### 3. ViewModel Logic

```kotlin
// Track added words
private val _addedWords = mutableStateOf<Set<String>>(emptySet())
val addedWords: State<Set<String>> = _addedWords

// Add word to vault
fun addWord(wordResult: WordResult) {
    viewModelScope.launch {
        val word = Word().apply {
            this.word = wordResult.word
            definition = wordResult.example
            translation = wordResult.meaning
        }
        apiService.addWord(word)
        _addedWords.value += wordResult.word
    }
}
```

---

## UI/UX Design

### 1. Message Bubble Styles

- User message: Blue bubble (right side) - existing
- AI word search message: Gray card (left side), each word as sub-card

### 2. Card Layout

```
┌─────────────────────────────────────┐
│ ephemeral /ɪˈfem(ə)rəl/             │
│ adj. 短暂的；转瞬即逝的               │
│ 例：Fame is ephemeral.              │
│                                     │
│ [ 添加 ]                            │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│ transient /ˈtrænziənt/              │
│ adj. 短暂的；临时的                   │
│ 例：Transient workers.              │
│                                     │
│ [ 已添加 ]  (gray, disabled)         │
└─────────────────────────────────────┘
```

### 3. Empty States

- First entry: "输入中文或英文搜索单词"
- Loading: Skeleton screen or loading indicator
- No results: "未找到相关单词，请换一个词试试"

---

## Error Handling

### 1. Network Errors

| Scenario | Handling |
|----------|----------|
| AI request fails | Show "网络错误，请重试" button |
| Add word fails | Toast error, button stays clickable |

### 2. Edge Cases

| Case | Handling |
|------|----------|
| Empty input | Don't send request |
| AI finds no words | Show "未找到相关单词，请换一个词试试" |
| Multiple words added | Each request independent |
| Rapid repeat clicks | Disable button immediately after click |
| Page back and return | Re-check if word exists |

### 3. Performance

- Check word existence: Use local cache or batch check
- Avoid API calls on every render

### 4. Data Consistency

- Update local state after successful add
- Refresh word list when returning to Vault page

---

## Implementation Priority

1. Backend: Add `word_search` mode and new prompt
2. Backend: Update response structure
3. Android: Create `WordResultCard` component
4. Android: Update `AIChatScreen` to render word results
5. Android: Add `addWord` logic in ViewModel
6. Testing: End-to-end flow

---

## Questions Resolved

- Q: How to differentiate from current AI Chat?
  A: Enhance existing AI Chat, add button below AI response

- Q: AI response format?
  A: Simplified - word, phonetic, meaning, one example

- Q: Add button behavior?
  A: State changes (添加 → 已添加), with toast feedback

- Q: Chinese input handling?
  A: Return multiple related words, each with add button

- Q: Word already in vault?
  A: Button shows "已添加" (gray, disabled)
