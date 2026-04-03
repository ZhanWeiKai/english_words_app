package com.englishword.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishword.BuildConfig
import com.englishword.data.RetrofitClient
import com.englishword.data.SSEClient
import com.englishword.data.SSEEvent
import com.englishword.data.model.AIChatRequest
import com.englishword.data.model.AIChatResponse
import com.englishword.data.model.ChatMessage
import com.englishword.data.model.Word
import com.englishword.data.model.WordResult
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class AIChatViewModel : ViewModel() {

    companion object {
        private const val TAG = "AIChatViewModel"
    }

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _conversationId = MutableStateFlow<String?>(null)
    val conversationId: StateFlow<String?> = _conversationId.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Track added words in current session
    private val _addedWords = MutableStateFlow<Set<String>>(emptySet())
    val addedWords: StateFlow<Set<String>> = _addedWords.asStateFlow()

    // Track words currently being added
    private val _addingWords = MutableStateFlow<Set<String>>(emptySet())
    val addingWords: StateFlow<Set<String>> = _addingWords.asStateFlow()

    // Store training words for the session
    private var trainingWordsList: List<String> = emptyList()

    private val apiService by lazy { RetrofitClient.getApiService() }

    // SSE client for streaming
    private val sseClient by lazy { SSEClient(BuildConfig.BASE_URL) }

    /**
     * Initialize with training words (training mode)
     */
    fun initTrainingMode(selectedWords: List<Word>) {
        if (selectedWords.isNotEmpty() && _messages.value.isEmpty()) {
            trainingWordsList = selectedWords.mapNotNull { it.word }

            val wordsList = trainingWordsList.joinToString(" / ")
            val bannerMsg = ChatMessage().apply {
                role = "assistant"
                content = "🎯 本轮训练考词\n\n$wordsList\n\n正在开始训练..."
            }
            _messages.value = listOf(bannerMsg)

            startTraining()
        }
    }

    /**
     * Start training by sending initial message to AI
     */
    private fun startTraining() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val request = AIChatRequest().apply {
                    message = "Let's start!"
                    mode = "word_training"
                    trainingWords = trainingWordsList
                }

                val response = apiService.chat(request)

                if (response.isSuccess && response.data != null) {
                    val chatResponse = response.data!!
                    _conversationId.value = chatResponse.conversationId

                    val aiMsg = ChatMessage().apply {
                        role = "assistant"
                        content = chatResponse.message
                        conversationId = chatResponse.conversationId
                    }
                    _messages.value = _messages.value + aiMsg
                } else {
                    _error.value = response.message ?: "Failed to start training"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Send message to AI (streaming output)
     */
    fun sendMessage(
        message: String,
        mode: String = "word_inquiry",
        targetWord: String? = null
    ) {
        if (message.isBlank()) return

        viewModelScope.launch {
            _error.value = null

            // 1. Add user message immediately
            val userMsg = ChatMessage().apply {
                role = "user"
                content = message
            }
            _messages.value = _messages.value + userMsg
            _isLoading.value = true

            try {
                val token = RetrofitClient.getStoredToken()
                if (token.isNullOrBlank()) {
                    _error.value = "Not authenticated"
                    addAIMessage("Please login first.")
                    _isLoading.value = false
                    return@launch
                }

                val requestBody = buildStreamRequestBody(message, mode, targetWord)
                val fullResponse = StringBuilder()
                var aiMsgAdded = false

                // 2. Collect streaming data
                sseClient.chatStream("ai/chat/stream", token, requestBody)
                    .collect { event ->
                        when (event) {
                            is SSEEvent.ConversationId -> {
                                _conversationId.value = event.id
                                Log.d(TAG, "Received conversationId: ${event.id}")
                            }
                            is SSEEvent.Message -> {
                                // 第一次收到消息时添加AI消息占位符
                                if (!aiMsgAdded) {
                                    addEmptyAIMessage()
                                    aiMsgAdded = true
                                }
                                // 追加消息内容
                                fullResponse.append(event.content)
                                updateLastAIMessage(fullResponse.toString())
                            }
                            is SSEEvent.Done -> {
                                Log.d(TAG, "Stream completed")
                            }
                        }
                    }

            } catch (e: Exception) {
                Log.e(TAG, "Stream error: ${e.message}", e)
                _error.value = e.message ?: "Network error"
                addAIMessage("Sorry, network error occurred.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Add empty AI message placeholder (for streaming)
     */
    private fun addEmptyAIMessage() {
        val aiMsg = ChatMessage().apply {
            role = "assistant"
            content = ""
            conversationId = _conversationId.value
        }
        _messages.value = _messages.value + aiMsg
    }

    /**
     * Update the last AI message content
     */
    private fun updateLastAIMessage(content: String) {
        val currentList = _messages.value.toMutableList()
        val lastIndex = currentList.lastIndex
        if (lastIndex >= 0 && currentList[lastIndex].role == "assistant") {
            currentList[lastIndex] = ChatMessage().apply {
                role = "assistant"
                this.content = content
                conversationId = _conversationId.value
            }
            _messages.value = currentList
        }
    }

    /**
     * Add a complete AI message (for errors)
     */
    private fun addAIMessage(content: String) {
        val aiMsg = ChatMessage().apply {
            role = "assistant"
            this.content = content
            conversationId = _conversationId.value
        }
        _messages.value = _messages.value + aiMsg
    }

    /**
     * Build JSON request body for streaming
     */
    private fun buildStreamRequestBody(message: String, mode: String, targetWord: String?): String {
        val json = JSONObject()
        json.put("message", message)
        json.put("mode", mode)
        _conversationId.value?.let { json.put("conversationId", it) }
        targetWord?.let { json.put("targetWord", it) }
        if (mode == "word_training") {
            json.put("trainingWords", JSONArray(trainingWordsList))
        }
        return json.toString()
    }

    /**
     * Add word to user's vault
     */
    fun addWord(wordResult: WordResult) {
        Log.d("AIChatViewModel", "=== addWord called === wordResult: $wordResult")
        val wordText = wordResult.word ?: return

        if (_addedWords.value.contains(wordText) || _addingWords.value.contains(wordText)) {
            Log.d("AIChatViewModel", "=== Word already added or adding: $wordText ===")
            return
        }

        viewModelScope.launch {
            _addingWords.value = _addingWords.value + wordText

            try {
                val word = Word().apply {
                    this.word = wordResult.word
                    this.phonetic = wordResult.phonetic
                    this.partOfSpeech = wordResult.partOfSpeech
                    this.definition = wordResult.example
                    this.translation = wordResult.meaning
                    this.status = "LEARNING"
                }

                val response = apiService.addWord(word).execute()

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    _addedWords.value = _addedWords.value + wordText
                    Log.d("AIChatViewModel", "=== Word added successfully: $wordText ===")
                } else {
                    _error.value = "添加失败"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "网络错误"
            } finally {
                _addingWords.value = _addingWords.value - wordText
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
        trainingWordsList = emptyList()
        _addedWords.value = emptySet()
        _addingWords.value = emptySet()
    }

    /**
     * Load an existing conversation by conversationId
     */
    fun loadExistingConversation(conversationId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val response = apiService.getConversationDetail(conversationId)
                if (response.isSuccess && response.data != null) {
                    val conversation = response.data!!
                    _conversationId.value = conversation.conversationId

                    val messagesJson = conversation.messages
                    if (!messagesJson.isNullOrBlank()) {
                        val jsonArray = JSONArray(messagesJson)
                        val loadedMessages = mutableListOf<ChatMessage>()
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            loadedMessages.add(ChatMessage().apply {
                                role = obj.optString("role")
                                content = obj.optString("content")
                                this.conversationId = conversationId
                            })
                        }
                        _messages.value = loadedMessages
                    }
                } else {
                    _error.value = response.message ?: "加载对话失败"
                }
            } catch (e: Exception) {
                _error.value = "加载失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
