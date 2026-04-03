package com.englishword.data

import android.util.Log
import com.englishword.BuildConfig
import com.englishword.data.api.ApiService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val TAG = "english_words"

    // Expose BASE_URL for SSE client
    val BASE_URL: String = BuildConfig.BASE_URL

    private var apiService: ApiService? = null
    private var tokenManager: TokenManager? = null

    fun init(tokenMgr: TokenManager) {
        Log.d(TAG, "=== RetrofitClient.init() called ===")
        Log.d(TAG, "BASE_URL: ${BuildConfig.BASE_URL}")
        tokenManager = tokenMgr

        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d(TAG, "HTTP: $message")
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val token = runBlocking {
                    tokenManager?.getToken()?.first()
                }
                Log.d(TAG, "Request interceptor - token: ${token?.take(20)}...")
                val original = chain.request()
                val request = if (token != null) {
                    original.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .method(original.method, original.body)
                        .build()
                } else {
                    Log.w(TAG, "No token available for request")
                    original
                }
                chain.proceed(request)
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()

        apiService = retrofit.create(ApiService::class.java)
        Log.d(TAG, "RetrofitClient initialized successfully")
    }

    fun getApiService(): ApiService {
        Log.d(TAG, "getApiService() called, initialized: ${apiService != null}")
        return apiService ?: throw IllegalStateException("RetrofitClient not initialized. Call init() first.")
    }

    /**
     * Get stored token for SSE connections
     */
    suspend fun getStoredToken(): String? {
        return tokenManager?.getToken()?.first()
    }
}
