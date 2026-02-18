# AI Chat Integration Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Integrate Android AIChatScreen with backend AI API to enable real AI chat responses.

**Architecture:** Android app calls backend `/api/ai/chat` endpoint which forwards requests to ZhipuAI. Backend handles API key securely, conversation history, and mode switching (inquiry/training).

**Tech Stack:** Kotlin, Retrofit2, Coroutines, Jetpack Compose, MVVM pattern

---

## Existing Flow (Do NOT Change)

- Backend `AIController` and `AIConversationService` are working
- Backend uses ZhipuAI with API key in `application.yml`
- Login/Register flow remains unchanged
- WordVaultScreen navigation unchanged

## Extension Points

- `Models.kt` - Add `AIChatResponse` data class
- `ApiService.kt` - Update chat endpoint return type
- `AIChatScreen.kt` - Add ViewModel and API integration

## Non-breaking Rules

- Do NOT change backend code
- Do NOT change existing login/word vault flows
- Do NOT modify other screen navigation

---

## Task 1: Add AIChatResponse Model

**Files:**
- Modify: `android-word-app/app/src/main/java/com/englishword/data/model/Models.kt`

**Step 1: Add AIChatResponse data class to Models.kt**

Add after `ChatMessage` class (around line 97):

```kotlin
/**
 * AI Chat response from backend
 */
data class AIChatResponse(
    var conversationId: String? = null,
    var message: String? = null,
    var suggestions: List<Suggestion>? = null
) {
    data class Suggestion(
        var type: String? = null,
        var word: String? = null,
        var label: String? = null
    )
}
```

**Step 2: Verify compilation**

Run: `gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add android-word-app/app/src/main/java/com/englishword/data/model/Models.kt
git commit -m "feat: add AIChatResponse model for API"
```

---

## Task 2: Update ApiService Chat Endpoint

**Files:**
- Modify: `android-word-app/app/src/main/java/com/englishword/data/api/ApiService.kt`

**Step 1: Update chat endpoint return type**

Replace line 68-69:

```kotlin
// OLD:
@POST("ai/chat")
fun chat(@Body request: Map<String, String>): Call<ApiResponse<ChatMessage>>
```

With:

```kotlin
// NEW:
@POST("ai/chat")
suspend fun chat(@Body request: AIChatRequest): ApiResponse<AIChatResponse>
```

**Step 2: Add AIChatRequest import and update method**

Add at top of file (after existing imports):

```kotlin
import com.englishword.data.model.AIChatRequest
import com.englishword.data.model.AIChatResponse
```

**Step 3: Add AIChatRequest data class to Models.kt**

Add to `Models.kt` after `AIChatResponse`:

```kotlin
/**
 * AI Chat request to backend
 */
data class AIChatRequest(
    var message: String? = null,
    var conversationId: String? = null,
    var mode: String? = null,  // "word_inquiry" or "word_training"
    var targetWord: String? = null,
    var scenario: String? = null
)
```

**Step 4: Verify compilation**

Run: `gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add android-word-app/app/src/main/java/com/englishword/data/api/ApiService.kt
git add android-word-app/app/src/main/java/com/englishword/data/model/Models.kt
git commit -m "feat: update ApiService chat endpoint with correct types"
```

---

## Task 3: Create AIChatViewModel

**Files:**
- Create: `android-word-app/app/src/main/java/com/englishword/ui/screens/AIChatViewModel.kt`

**Step 1: Create AIChatViewModel.kt**

```kotlin
package com.englishword.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishword.data.RetrofitClient
import com.englishword.data.model.AIChatRequest
import com.englishword.data.model.AIChatResponse
import com.englishword.data.model.ChatMessage
import com.englishword.data.model.Word
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AIChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _conversationId = MutableStateFlow<String?>(null)
    val conversationId: StateFlow<String?> = _conversationId.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val apiService = RetrofitClient.apiService

    /**
     * Initialize with training words (training mode)
     */
    fun initTrainingMode(selectedWords: List<Word>) {
        if (selectedWords.isNotEmpty() && _messages.value.isEmpty()) {
            val wordsList = selectedWords.mapNotNull { it.word }.joinToString(", ")
            val welcomeMsg = ChatMessage().apply {
                role = "assistant"
                content = "Welcome to Word Training! You'll practice using these words: $wordsList\n\nI'll help you create sentences and have conversations using these words. Let's start! Try to make a sentence using one of these words."
            }
            _messages.value = listOf(welcomeMsg)
        }
    }

    /**
     * Send message to AI
     */
    fun sendMessage(
        message: String,
        mode: String = "word_inquiry",
        targetWord: String? = null
    ) {
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
                    this.conversationId = _conversationId.value
                    this.mode = mode
                    this.targetWord = targetWord
                }

                val response = apiService.chat(request)

                if (response.isSuccess && response.data != null) {
                    val chatResponse = response.data!!

                    // Update conversation ID
                    _conversationId.value = chatResponse.conversationId

                    // Add AI response
                    val aiMsg = ChatMessage().apply {
                        role = "assistant"
                        content = chatResponse.message
                        conversationId = chatResponse.conversationId
                    }
                    _messages.value = _messages.value + aiMsg
                } else {
                    _error.value = response.message ?: "Failed to get AI response"
                    // Add error message as AI response
                    val errorMsg = ChatMessage().apply {
                        role = "assistant"
                        content = "Sorry, I encountered an error. Please try again."
                    }
                    _messages.value = _messages.value + errorMsg
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
                // Add error message as AI response
                val errorMsg = ChatMessage().apply {
                    role = "assistant"
                    content = "Sorry, network error occurred. Please check your connection."
                }
                _messages.value = _messages.value + errorMsg
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear conversation
     */
    fun clearConversation() {
        _messages.value = emptyList()
        _conversationId.value = null
        _error.value = null
    }
}
```

**Step 2: Verify compilation**

Run: `gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add android-word-app/app/src/main/java/com/englishword/ui/screens/AIChatViewModel.kt
git commit -m "feat: add AIChatViewModel for API integration"
```

---

## Task 4: Update AIChatScreen to Use ViewModel

**Files:**
- Modify: `android-word-app/app/src/main/java/com/englishword/ui/screens/AIChatScreen.kt`

**Step 1: Add imports**

Add after existing imports (around line 36):

```kotlin
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
```

**Step 2: Update AIChatScreen function**

Replace the entire `AIChatScreen` composable function (lines 50-161) with:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(
    onBack: () -> Unit,
    selectedWords: List<Word> = emptyList()
) {
    val viewModel: AIChatViewModel = viewModel()
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Check if this is training mode
    val isTrainingMode = selectedWords.isNotEmpty()

    // Initialize training mode with welcome message
    LaunchedEffect(isTrainingMode) {
        if (isTrainingMode) {
            viewModel.initTrainingMode(selectedWords)
        }
    }

    // Auto-scroll to bottom when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Voice input state
    var isVoiceMode by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var isCancelling by remember { mutableStateOf(false) }

    val view = LocalView.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isTrainingMode) "Word Training" else "AI Chat") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Training words banner
            if (isTrainingMode) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Training Words:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedWords.mapNotNull { it.word }.joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Messages list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    ChatMessageItem(message = message)
                }

                // Loading indicator
                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("AI is thinking...")
                                }
                            }
                        }
                    }
                }
            }

            // Input area
            if (isVoiceMode) {
                VoiceInputArea(
                    isRecording = isRecording,
                    isCancelling = isCancelling,
                    onRecordStart = {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        isRecording = true
                        isCancelling = false
                    },
                    onRecordEnd = { cancelled ->
                        if (cancelled) {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        } else {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        }
                        isRecording = false
                        isCancelling = false
                    },
                    onCancelChange = { cancelling ->
                        if (cancelling != isCancelling) {
                            isCancelling = cancelling
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                    },
                    onSwitchToText = {
                        isRecording = false
                        isCancelling = false
                        isVoiceMode = false
                    }
                )
            } else {
                var inputText by remember { mutableStateOf("") }

                TextInputArea(
                    inputText = inputText,
                    onInputChange = { inputText = it },
                    onSend = {
                        if (inputText.isNotBlank() && !isLoading) {
                            val targetWord = if (isTrainingMode) {
                                selectedWords.firstOrNull()?.word
                            } else null

                            viewModel.sendMessage(
                                message = inputText,
                                mode = if (isTrainingMode) "word_training" else "word_inquiry",
                                targetWord = targetWord
                            )
                            inputText = ""
                        }
                    },
                    onSwitchToVoice = { isVoiceMode = true }
                )
            }
        }
    }
}
```

**Step 3: Remove old LaunchedEffect for training mode**

Delete the old LaunchedEffect block (lines 63-73 in original) since it's now handled in ViewModel.

**Step 4: Verify compilation**

Run: `gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add android-word-app/app/src/main/java/com/englishword/ui/screens/AIChatScreen.kt
git commit -m "feat: integrate AIChatViewModel into AIChatScreen"
```

---

## Task 5: Build and Deploy for Testing

**Step 1: Build APK**

Run:
```bash
cd android-word-app
gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL

**Step 2: Transfer APK to device**

```bash
scp -P 8022 -i ~/.ssh/id_ed25519 android-word-app/app/build/outputs/apk/debug/app-debug.apk u0_a316@100.66.50.1:storage/downloads/
```

**Step 3: Test scenarios**

1. **Normal Chat Test:**
   - Open app, login
   - Tap AI Chat tab
   - Send: "What does 'ephemeral' mean?"
   - Expected: AI responds with word explanation

2. **Training Mode Test:**
   - Go to Word Vault
   - Long press a word to enter multi-select
   - Select 2-3 words
   - Tap Start Training FAB
   - Send a sentence using the training words
   - Expected: AI responds in training context

**Step 4: Final commit**

```bash
git add -A
git commit -m "feat: implement AI chat integration with backend API"
```

---

## Risk Points

1. **Network timeout** - Handled with try-catch in ViewModel
2. **Token expiration** - RetrofitClient handles token refresh
3. **Empty response** - Gracefully handled with error message

## Rollback Plan

If issues occur:
```bash
git revert HEAD
```
