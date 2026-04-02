package com.englishword.data

import android.util.Log
import com.englishword.data.api.ApiService
import com.englishword.data.model.ASRResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

object ASRService {
    private const val TAG = "ASRService"

    /**
     * 识别语音文件
     * @param audioFilePath 音频文件路径
     * @return 识别出的文本，失败返回null
     */
    suspend fun recognizeSpeech(
        apiService: ApiService,
        audioFilePath: String,
        language: String? = null
    ): String? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(audioFilePath)
                if (!file.exists()) {
                    Log.e(TAG, "Audio file not found: $audioFilePath")
                    return@withContext null
                }

                Log.d(TAG, "Sending audio file: $audioFilePath, size: ${file.length()} bytes")

                // Create multipart request
                val mediaType = "audio/mp4".toMediaType()
                val requestBody = file.asRequestBody(mediaType)
                val filePart = MultipartBody.Part.createFormData("file", file.name, requestBody)

                val languagePart = language?.let {
                    MultipartBody.Part.createFormData("language", it)
                }

                val response = if (languagePart != null) {
                    apiService.recognizeSpeech(filePart, language)
                } else {
                    apiService.recognizeSpeech(filePart, null)
                }

                Log.d(TAG, "ASR response: language=${response.language}, text=${response.text}")
                response.text

            } catch (e: Exception) {
                Log.e(TAG, "ASR recognition failed", e)
                null
            }
        }
    }
}
