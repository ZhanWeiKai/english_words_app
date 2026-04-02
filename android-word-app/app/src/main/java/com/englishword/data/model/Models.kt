package com.englishword.data.model

import com.google.gson.annotations.SerializedName

/**
 * API Response wrapper
 */
data class ApiResponse<T>(
    val code: Int = 0,
    val message: String? = null,
    val data: T? = null
) {
    val isSuccess: Boolean
        get() = code == 200
}

/**
 * Word List Response - 专门用于单词列表，避免泛型问题
 */
data class WordListResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: List<Word>? = null
) {
    val isSuccess: Boolean
        get() = code == 200
}

/**
 * User model
 */
data class User(
    var id: String? = null,
    var username: String? = null,
    var password: String? = null,
    var createdAt: String? = null
)

/**
 * Word model - 简化版本，避免 Gson 序列化问题
 */
data class Word(
    @SerializedName("wordId")
    var id: String? = null,

    @SerializedName("userId")
    var userId: String? = null,

    @SerializedName("word")
    var word: String? = null,

    @SerializedName("pronunciation")
    var phonetic: String? = null,

    @SerializedName("definition")
    var definition: String? = null,

    @SerializedName("translation")
    var translation: String? = null,

    @SerializedName("exampleSentence")
    var example: String? = null,

    @SerializedName("exampleTranslation")
    var exampleTranslation: String? = null,

    @SerializedName("partOfSpeech")
    var partOfSpeech: String? = null,

    @SerializedName("masteryLevel")
    var masteryLevel: Int = 1,

    @SerializedName("status")
    var status: String? = null,

    var reviewCount: Int = 0,

    @SerializedName("createdAt")
    var createdAt: String? = null,

    var lastReviewedAt: String? = null
) {
    // 计算属性，不影响序列化
    val masteryLevelValue: Int
        get() = masteryLevel
}

/**
 * Login request model
 */
data class LoginRequest(
    val username: String,
    val password: String
)

/**
 * Login response model
 */
data class LoginResponse(
    val token: String? = null,
    val userId: String? = null,
    val username: String? = null
)

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

/**
 * AI Chat request to backend
 */
data class AIChatRequest(
    var message: String? = null,
    var conversationId: String? = null,
    var mode: String? = null,  // "word_inquiry", "word_training", or "word_search"
    var targetWord: String? = null,
    var scenario: String? = null,
    var trainingWords: List<String>? = null  // 训练单词列表
)

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

/**
 * Spring Data Page response wrapper
 */
data class PageResponse<T>(
    val content: List<T>? = null,
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val size: Int = 0,
    val number: Int = 0,
    val first: Boolean = true,
    val last: Boolean = true,
    val empty: Boolean = true
)

/**
 * AI Conversation model for conversation list
 */
data class AIConversation(
    @SerializedName("conversationId")
    var conversationId: String? = null,

    @SerializedName("userId")
    var userId: String? = null,

    @SerializedName("messages")
    var messages: String? = null,  // JSON string

    @SerializedName("contextWordId")
    var contextWordId: String? = null,

    @SerializedName("createdAt")
    var createdAt: String? = null,

    @SerializedName("updatedAt")
    var updatedAt: String? = null
) {
    /**
     * Get preview text from messages (first user message or empty)
     */
    fun getPreview(): String {
        // Try to extract first user message from JSON
        // Simple approach: just show date if parsing is complex
        return createdAt?.takeIf { it.isNotEmpty() } ?: "对话"
    }
}

/**
 * ASR (Speech Recognition) Response
 */
data class ASRResponse(
    val language: String? = null,
    val text: String? = null
)
