package com.englishword.data.model

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
 * User model
 */
data class User(
    var id: String? = null,
    var username: String? = null,
    var password: String? = null,
    var createdAt: String? = null
)

/**
 * Word model
 */
data class Word(
    var id: String? = null,
    var userId: String? = null,
    var word: String? = null,
    var phonetic: String? = null,
    var definition: String? = null,
    var translation: String? = null,
    var example: String? = null,
    var exampleTranslation: String? = null,
    var partOfSpeech: String? = null,
    var masteryLevel: MasteryLevel? = null,
    var status: String? = null,
    var reviewCount: Int = 0,
    var createdAt: String? = null,
    var lastReviewedAt: String? = null
) {
    enum class MasteryLevel(val level: Int) {
        BEGINNER(1),
        ELEMENTARY(2),
        INTERMEDIATE(3),
        ADVANCED(4),
        PROFICIENT(5);

        companion object {
            fun fromLevel(level: Int): MasteryLevel {
                return values().find { it.level == level } ?: BEGINNER
            }
        }
    }

    var masteryLevelValue: Int
        get() = masteryLevel?.level ?: 1
        set(value) {
            masteryLevel = MasteryLevel.fromLevel(value)
        }
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
    var createdAt: String? = null
) {
    val isUser: Boolean
        get() = role == "user"

    val isAssistant: Boolean
        get() = role == "assistant"
}
