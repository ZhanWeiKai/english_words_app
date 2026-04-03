package com.englishword.data

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.englishword.data.api.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

object TTSService {
    private const val TAG = "tts_tag"
    private var mediaPlayer: MediaPlayer? = null

    // URL缓存：text -> audioUrl
    private val urlCache = ConcurrentHashMap<String, String>()

    /**
     * 合成语音并播放
     * @param apiService API服务
     * @param text 要合成的文本
     * @param onComplete 播放完成回调
     * @param onUrlReady URL获取成功回调（用于调试）
     */
    suspend fun speak(
        apiService: ApiService,
        text: String,
        onComplete: (() -> Unit)? = null,
        onUrlReady: ((String) -> Unit)? = null
    ): Boolean {
        Log.d(TAG, "=== speak() called === text: ${text.take(50)}...")
        return withContext(Dispatchers.IO) {
            try {
                // 先检查缓存
                val cachedUrl = urlCache[text]
                if (!cachedUrl.isNullOrBlank()) {
                    Log.d(TAG, "Using cached URL: $cachedUrl")
                    onUrlReady?.invoke("(cached) $cachedUrl")
                    withContext(Dispatchers.Main) {
                        playAudio(cachedUrl, onComplete)
                    }
                    return@withContext true
                }

                // 缓存未命中，调用TTS接口获取音频URL
                Log.d(TAG, "Cache miss, calling API...")
                val response = apiService.synthesizeSpeech(text)
                Log.d(TAG, "API response received: $response")

                val audioUrl = response.audioUrl
                Log.d(TAG, "Extracted audioUrl: $audioUrl")

                if (audioUrl.isNullOrBlank()) {
                    Log.e(TAG, "ERROR: audioUrl is null or blank!")
                    return@withContext false
                }

                // 缓存URL
                urlCache[text] = audioUrl
                Log.d(TAG, "URL cached successfully")

                // 回调通知URL
                Log.d(TAG, "Calling onUrlReady callback...")
                onUrlReady?.invoke(audioUrl)

                // 在主线程播放音频
                withContext(Dispatchers.Main) {
                    playAudio(audioUrl, onComplete)
                }

                true
            } catch (e: Exception) {
                Log.e(TAG, "ERROR: TTS synthesis failed", e)
                false
            }
        }
    }

    /**
     * 直接使用URL播放（用于已有缓存的情况）
     */
    fun playWithUrl(url: String, onComplete: (() -> Unit)? = null) {
        playAudio(url, onComplete)
    }

    /**
     * 获取缓存的URL
     */
    fun getCachedUrl(text: String): String? = urlCache[text]

    /**
     * 清除缓存
     */
    fun clearCache() {
        urlCache.clear()
        Log.d(TAG, "TTS URL cache cleared")
    }

    /**
     * 播放音频URL
     */
    private fun playAudio(url: String, onComplete: (() -> Unit)?) {
        Log.d(TAG, "=== playAudio() called === url: $url")
        // 停止之前的播放
        stopPlaying()

        try {
            Log.d(TAG, "Creating MediaPlayer...")
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                Log.d(TAG, "Setting data source: $url")
                setDataSource(url)
                setOnCompletionListener {
                    Log.d(TAG, "Audio playback completed")
                    stopPlaying()
                    onComplete?.invoke()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    stopPlaying()
                    onComplete?.invoke()
                    true
                }
                prepareAsync()
                setOnPreparedListener {
                    Log.d(TAG, "MediaPlayer prepared, starting playback")
                    start()
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to play audio", e)
            onComplete?.invoke()
        }
    }

    /**
     * 停止播放
     */
    fun stopPlaying() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }

    /**
     * 是否正在播放
     */
    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }
}
