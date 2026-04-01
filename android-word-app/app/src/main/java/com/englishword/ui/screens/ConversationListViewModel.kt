package com.englishword.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishword.data.RetrofitClient
import com.englishword.data.model.AIConversation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TAG = "ConversationList"

class ConversationListViewModel : ViewModel() {

    private val apiService by lazy { RetrofitClient.getApiService() }

    private val _conversations = MutableStateFlow<List<AIConversation>>(emptyList())
    val conversations: StateFlow<List<AIConversation>> = _conversations

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private var currentPage = 0
    private var hasMore = true

    init {
        loadConversations()
    }

    fun loadConversations() {
        if (_isLoading.value || !hasMore) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = apiService.getConversationList(page = currentPage, size = 10)
                if (response.isSuccess) {
                    val newConversations = response.data ?: emptyList()
                    if (currentPage == 0) {
                        _conversations.value = newConversations
                    } else {
                        // Deduplicate by conversationId
                        val existingIds = _conversations.value.mapNotNull { it.conversationId }.toSet()
                        val uniqueNew = newConversations.filter { it.conversationId !in existingIds }
                        _conversations.value = _conversations.value + uniqueNew
                        Log.d(TAG, "Filtered ${newConversations.size - uniqueNew.size} duplicates")
                    }
                    // Stop loading when returned fewer than requested
                    hasMore = newConversations.size >= 10
                    Log.d(TAG, "Loaded page $currentPage: ${newConversations.size} items, hasMore=$hasMore, total=${_conversations.value.size}")
                } else {
                    _errorMessage.value = response.message ?: "加载失败"
                    hasMore = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load conversations", e)
                _errorMessage.value = "网络错误: ${e.message}"
                hasMore = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMore() {
        if (_isLoading.value || !hasMore) return
        currentPage++
        loadConversations()
    }

    fun refresh() {
        currentPage = 0
        hasMore = true
        loadConversations()
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
