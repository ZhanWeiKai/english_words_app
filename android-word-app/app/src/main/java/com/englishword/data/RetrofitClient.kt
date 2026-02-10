package com.englishword.data

import com.englishword.BuildConfig
import com.englishword.data.api.ApiService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private var apiService: ApiService? = null
    private var tokenManager: TokenManager? = null

    fun init(tokenMgr: TokenManager) {
        tokenManager = tokenMgr

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val token = runBlocking {
                    tokenManager?.getToken()?.first()
                }
                val original = chain.request()
                val request = if (token != null) {
                    original.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .method(original.method, original.body)
                        .build()
                } else {
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
    }

    fun getApiService(): ApiService {
        return apiService ?: throw IllegalStateException("RetrofitClient not initialized. Call init() first.")
    }
}
