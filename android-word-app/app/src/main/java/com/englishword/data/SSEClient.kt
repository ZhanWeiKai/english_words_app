package com.englishword.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * SSE事件类型
 */
sealed class SSEEvent {
    data class ConversationId(val id: String) : SSEEvent()
    data class Message(val content: String) : SSEEvent()
    object Done : SSEEvent()
}

class SSEClient(private val baseUrl: String) {

    companion object {
        private const val TAG = "SSEClient"
    }

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)  // 流式读取不设超时
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * 流式聊天 - 使用原始流读取以保留空格
     * 支持两种事件类型:
     * - event:conversationId + data:xxx (会话ID)
     * - event:message + data:xxx (消息内容)
     */
    fun chatStream(endpoint: String, token: String, requestBody: String): Flow<SSEEvent> = channelFlow {
        launch(Dispatchers.IO) {
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestBody.toRequestBody(mediaType)

            val url = "$baseUrl$endpoint"
            Log.d(TAG, "Starting SSE connection to: $url")

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Accept", "text/event-stream")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            try {
                val call = client.newCall(request)
                val response = call.execute()

                Log.d(TAG, "SSE response code: ${response.code}")

                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "SSE request failed: ${response.code} - $errorBody")
                    close(Exception("HTTP ${response.code}: $errorBody"))
                    return@launch
                }

                val reader = response.body?.byteStream()?.bufferedReader()
                if (reader == null) {
                    Log.e(TAG, "Response body is null")
                    close(Exception("Response body is null"))
                    return@launch
                }

                try {
                    var currentEventType: String? = null
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        val currentLine = line ?: continue

                        // 跳过空行
                        if (currentLine.isEmpty()) continue

                        Log.d(TAG, "SSE raw line: ${currentLine.take(100)}")

                        when {
                            // 解析 event: 行
                            currentLine.startsWith("event:") -> {
                                currentEventType = currentLine.substring(6).trim()
                                Log.d(TAG, "SSE event type: $currentEventType")
                            }

                            // 解析 data: 行
                            currentLine.startsWith("data:") -> {
                                val data = currentLine.substring(5) // 保留前导空格

                                if (data == "[DONE]") {
                                    Log.d(TAG, "SSE stream completed")
                                    send(SSEEvent.Done)
                                    close()
                                    return@launch
                                }

                                when (currentEventType) {
                                    "conversationId" -> {
                                        Log.d(TAG, "SSE conversationId: $data")
                                        send(SSEEvent.ConversationId(data))
                                    }
                                    "message" -> {
                                        Log.d(TAG, "SSE message: '$data' len=${data.length}")
                                        send(SSEEvent.Message(data))
                                    }
                                    else -> {
                                        Log.d(TAG, "SSE unknown event type: $currentEventType, data: $data")
                                    }
                                }
                            }
                        }
                    }
                    Log.d(TAG, "SSE stream ended normally")
                    close()
                } finally {
                    reader.close()
                    response.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "SSE error: ${e.message}", e)
                close(e)
            }
        }

        awaitClose {
            Log.d(TAG, "SSE flow closed")
        }
    }
}
