package com.englishword.data.api

import com.englishword.data.model.*
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
    ): Call<ApiResponse<List<Word>>>

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
    ): Call<ApiResponse<List<Word>>>

    @PUT("words/{wordId}/mastery")
    fun updateMasteryLevel(
        @Path("wordId") wordId: String,
        @Query("masteryLevel") masteryLevel: Int
    ): Call<ApiResponse<Word>>

    @GET("words/count")
    fun countByStatus(@Query("status") status: String): Call<ApiResponse<Long>>

    // ==================== AI Chat APIs ====================

    @POST("ai/chat")
    fun chat(@Body request: Map<String, String>): Call<ApiResponse<ChatMessage>>

    @GET("ai/conversations")
    fun getConversationHistory(
        @Query("conversationId") conversationId: String?
    ): Call<ApiResponse<List<ChatMessage>>>
}
