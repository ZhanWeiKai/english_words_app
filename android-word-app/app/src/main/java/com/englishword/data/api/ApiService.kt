package com.englishword.data.api

import com.englishword.data.model.*
import com.englishword.data.model.AIChatRequest
import com.englishword.data.model.AIChatResponse
import com.englishword.data.model.WordListResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

/**
 * API Service interfaces
 */
interface ApiService {

    // ==================== Auth APIs ====================

    @POST("auth/register")
    fun register(@Body user: User): Call<ApiResponse<LoginResponse>>

    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<ApiResponse<LoginResponse>>

    @POST("auth/logout")
    fun logout(): Call<ApiResponse<String>>

    @GET("auth/me")
    fun getCurrentUser(): Call<ApiResponse<User>>

    // ==================== Word APIs ====================

    @POST("words")
    fun addWord(@Body word: Word): Call<ApiResponse<Word>>

    @GET("words")
    fun getUserWords(
        @Query("status") status: String?,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Call<WordListResponse>

    @GET("words/{wordId}")
    fun getWordById(@Path("wordId") wordId: String): Call<ApiResponse<Word>>

    @PUT("words/{wordId}")
    fun updateWord(
        @Path("wordId") wordId: String,
        @Body word: Word
    ): Call<ApiResponse<Word>>

    @DELETE("words/{wordId}")
    fun deleteWord(@Path("wordId") wordId: String): Call<ApiResponse<String>>

    @GET("words/search")
    fun searchWords(
        @Query("keyword") keyword: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Call<WordListResponse>

    @PUT("words/{wordId}/mastery")
    fun updateMasteryLevel(
        @Path("wordId") wordId: String,
        @Query("masteryLevel") masteryLevel: Int
    ): Call<ApiResponse<Word>>

    @GET("words/count")
    fun countByStatus(@Query("status") status: String): Call<ApiResponse<Long>>

    // ==================== AI Chat APIs ====================

    @POST("ai/chat")
    suspend fun chat(@Body request: AIChatRequest): ApiResponse<AIChatResponse>

    @GET("ai/conversations")
    fun getConversationHistory(
        @Query("conversationId") conversationId: String?
    ): Call<ApiResponse<List<ChatMessage>>>

    @GET("ai/conversations")
    suspend fun getConversationList(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): ApiResponse<List<AIConversation>>

    @GET("ai/conversations/{conversationId}")
    suspend fun getConversationDetail(
        @Path("conversationId") conversationId: String
    ): ApiResponse<AIConversation>

    // ==================== ASR APIs ====================

    @Multipart
    @POST("asr/recognize")
    suspend fun recognizeSpeech(
        @Part file: MultipartBody.Part,
        @Part("language") language: String?
    ): ASRResponse

    // ==================== TTS APIs ====================

    @POST("tts/synthesize")
    suspend fun synthesizeSpeech(
        @Query("text") text: String
    ): TTSResponse

    // ==================== Sentence APIs ====================

    @GET("sentences")
    suspend fun getSentences(
        @Query("keyword") keyword: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): SentenceListResponse

    @DELETE("sentences/{sentenceId}")
    suspend fun deleteSentence(
        @Path("sentenceId") sentenceId: String
    ): SentenceDeleteResponse
}
