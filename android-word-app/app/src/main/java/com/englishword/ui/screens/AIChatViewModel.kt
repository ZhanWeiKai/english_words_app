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

    // Store training words for the session
    private var trainingWordsList: List<String> = emptyList()

    private val apiService by lazy { RetrofitClient.getApiService() }

    /**
     * Initialize with training words (training mode)
     * This sends "Let's start!" to trigger the first question from AI
     */
    fun initTrainingMode(selectedWords: List<Word>) {
        if (selectedWords.isNotEmpty() && _messages.value.isEmpty()) {
            // Store training words
            trainingWordsList = selectedWords.mapNotNull { it.word }

            // Show training words banner
            val wordsList = trainingWordsList.joinToString(" / ")
            val bannerMsg = ChatMessage().apply {
                role = "assistant"
                content = "🎯 本轮训练考词\n\n$wordsList\n\n正在开始训练..."
            }
            _messages.value = listOf(bannerMsg)

            // Send "Let's start!" to get first question from AI
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

                    // Add AI's first question
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
                    // Include training words for training mode
                    if (mode == "word_training") {
                        this.trainingWords = trainingWordsList
                    }
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
        trainingWordsList = emptyList()
    }
}
