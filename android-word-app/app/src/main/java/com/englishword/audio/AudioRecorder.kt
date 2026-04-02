package com.englishword.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudioRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var isRecording = false

    companion object {
        private const val TAG = "AudioRecorder"
        private const val AUDIO_DIR = "EnglishWord/recordings"
    }

    /**
     * 获取录音保存目录（外部存储公共目录）
     */
    private fun getRecordingDir(): File {
        // 使用外部存储的 Music 目录，用户可以通过文件管理器访问
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val audioDir = File(musicDir, AUDIO_DIR)
        if (!audioDir.exists()) {
            audioDir.mkdirs()
        }
        return audioDir
    }

    /**
     * 开始录音
     * @return 录音文件路径，失败返回null
     */
    fun startRecording(): String? {
        if (isRecording) {
            Log.w(TAG, "Already recording")
            return null
        }

        try {
            // 创建录音文件目录
            val audioDir = getRecordingDir()

            // 生成带时间戳的文件名
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            outputFile = File(audioDir, "voice_$timestamp.m4a")

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile!!.absolutePath)

                prepare()
                start()
            }

            isRecording = true
            Log.d(TAG, "Recording started: ${outputFile!!.absolutePath}")
            return outputFile!!.absolutePath

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            releaseRecorder()
            return null
        }
    }

    /**
     * 停止录音
     * @return 录音文件路径，失败返回null
     */
    fun stopRecording(): String? {
        if (!isRecording) {
            Log.w(TAG, "Not recording")
            return null
        }

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false

            val path = outputFile?.absolutePath
            Log.d(TAG, "Recording stopped: $path, file size: ${outputFile?.length()} bytes")
            return path

        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
            releaseRecorder()
            return null
        }
    }

    /**
     * 取消录音（删除文件）
     */
    fun cancelRecording() {
        if (!isRecording) {
            return
        }

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false

            // 删除录音文件
            outputFile?.delete()
            Log.d(TAG, "Recording cancelled, file deleted")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel recording", e)
            releaseRecorder()
        }
    }

    private fun releaseRecorder() {
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing recorder", e)
        }
        mediaRecorder = null
        isRecording = false
    }

    fun isRecording(): Boolean = isRecording

    /**
     * 获取录音目录路径（外部存储公共目录）
     */
    fun getRecordingDirPath(): String {
        return getRecordingDir().absolutePath
    }
}
