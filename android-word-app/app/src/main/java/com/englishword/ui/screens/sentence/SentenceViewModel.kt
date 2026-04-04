package com.englishword.ui.screens.sentence

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishword.data.RetrofitClient
import com.englishword.data.model.Sentence
import com.englishword.data.model.SentenceListResponse
import com.englishword.data.model.SentenceDeleteResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SentenceViewModel : ViewModel() {

    companion object {
        private const val TAG = "SentenceViewModel"
    }

    private val _sentences = MutableStateFlow<List<Sentence>>(emptyList())
    val sentences: StateFlow<List<Sentence>> = _sentences.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentKeyword: String? = null

    fun loadSentences(keyword: String? = null, page: Int = 0, size: Int = 20) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            currentKeyword = keyword

            try {
                // Add small delay to ensure RetrofitClient is initialized
                kotlinx.coroutines.delay(100)
                Log.d(TAG, "Loading sentences: keyword=$keyword, page=$page, size=$size")
                val response: SentenceListResponse = RetrofitClient.getApiService().getSentences(keyword, page, size)
                Log.d(TAG, "Response received: code=${response.code}, success=${response.success}, data size=${response.data?.size}")

                if (response.success && response.data != null) {
                    _sentences.value = response.data
                    Log.d(TAG, "Loaded ${response.data.size} sentences")
                } else {
                    _error.value = response.message ?: "Failed to load sentences"
                    _sentences.value = emptyList()
                }
            } catch (e: IllegalStateException) {
                Log.e(TAG, "RetrofitClient not initialized yet, retrying...")
                // Retry after a longer delay
                kotlinx.coroutines.delay(500)
                try {
                    val response: SentenceListResponse = RetrofitClient.getApiService().getSentences(keyword, page, size)
                    if (response.success && response.data != null) {
                        _sentences.value = response.data
                        Log.d(TAG, "Loaded ${response.data.size} sentences on retry")
                    } else {
                        _error.value = response.message ?: "Failed to load sentences"
                        _sentences.value = emptyList()
                    }
                } catch (e2: Exception) {
                    Log.e(TAG, "Error loading sentences on retry", e2)
                    _error.value = e2.message ?: "Network error"
                    _sentences.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading sentences", e)
                _error.value = e.message ?: "Network error"
                _sentences.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchSentences(keyword: String) {
        loadSentences(keyword = keyword.ifBlank { null }, page = 0)
    }

    fun deleteSentence(sentenceId: String) {
        viewModelScope.launch {
            try {
                val response: SentenceDeleteResponse = RetrofitClient.getApiService().deleteSentence(sentenceId)
                if (response.isSuccess) {
                    // Remove from local list
                    _sentences.value = _sentences.value.filter { it.id != sentenceId }
                    Log.d(TAG, "Sentence deleted: $sentenceId")
                } else {
                    _error.value = response.message ?: "Failed to delete sentence"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting sentence", e)
                _error.value = e.message ?: "Network error"
            }
        }
    }

    fun refresh() {
        loadSentences(keyword = currentKeyword)
    }

    fun clearError() {
        _error.value = null
    }
}
