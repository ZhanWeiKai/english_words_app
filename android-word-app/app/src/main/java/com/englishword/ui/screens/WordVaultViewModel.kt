package com.englishword.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishword.data.RetrofitClient
import com.englishword.data.model.WordListResponse
import com.englishword.data.model.Word
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class WordVaultViewModel : ViewModel() {

    companion object {
        private const val TAG = "english_words"
    }

    // UI State
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Words list
    private val _words = MutableStateFlow<List<Word>>(emptyList())
    val words: StateFlow<List<Word>> = _words.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error message
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    sealed class UiState {
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }

    /**
     * Load words from API
     */
    fun loadWords() {
        Log.d(TAG, "=== loadWords() called ===")
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                Log.d(TAG, "Calling API: getUserWords(status=null, page=0, size=100)")
                RetrofitClient.getApiService().getUserWords(
                    status = null,
                    page = 0,
                    size = 100
                ).enqueue(object : Callback<WordListResponse> {
                    override fun onResponse(
                        call: Call<WordListResponse>,
                        response: Response<WordListResponse>
                    ) {
                        Log.d(TAG, "=== onResponse ===")
                        Log.d(TAG, "isSuccessful: ${response.isSuccessful}")
                        Log.d(TAG, "code: ${response.code()}")
                        Log.d(TAG, "raw response: ${response.raw()}")

                        _isLoading.value = false

                        if (response.isSuccessful) {
                            val apiResponse = response.body()
                            Log.d(TAG, "apiResponse: $apiResponse")
                            Log.d(TAG, "apiResponse.code: ${apiResponse?.code}")
                            Log.d(TAG, "apiResponse.message: ${apiResponse?.message}")
                            Log.d(TAG, "apiResponse.data: ${apiResponse?.data}")
                            Log.d(TAG, "apiResponse.data size: ${apiResponse?.data?.size}")

                            if (apiResponse?.isSuccess == true && apiResponse.data != null) {
                                _words.value = apiResponse.data
                                _uiState.value = UiState.Success
                                Log.d(TAG, "SUCCESS: Loaded ${apiResponse.data.size} words")
                                apiResponse.data.forEachIndexed { index, word ->
                                    Log.d(TAG, "Word[$index]: ${word.word} - ${word.definition}")
                                }
                            } else {
                                _errorMessage.value = apiResponse?.message ?: "Failed to load words"
                                _uiState.value = UiState.Error(_errorMessage.value!!)
                                Log.e(TAG, "API ERROR: ${_errorMessage.value}")
                            }
                        } else {
                            val errorBody = response.errorBody()?.string()
                            _errorMessage.value = "Server error: ${response.code()}"
                            _uiState.value = UiState.Error(_errorMessage.value!!)
                            Log.e(TAG, "HTTP ERROR: ${response.code()}, body: $errorBody")
                        }
                    }

                    override fun onFailure(call: Call<WordListResponse>, t: Throwable) {
                        Log.e(TAG, "=== onFailure ===")
                        Log.e(TAG, "Exception: ${t.javaClass.simpleName}")
                        Log.e(TAG, "Message: ${t.message}")
                        t.printStackTrace()
                        _isLoading.value = false
                        _errorMessage.value = "Network error: ${t.message}"
                        _uiState.value = UiState.Error(_errorMessage.value!!)
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "=== Exception in loadWords ===")
                Log.e(TAG, "Exception: ${e.javaClass.simpleName}")
                Log.e(TAG, "Message: ${e.message}")
                e.printStackTrace()
                _isLoading.value = false
                _errorMessage.value = "Error: ${e.message}"
                _uiState.value = UiState.Error(_errorMessage.value!!)
            }
        }
    }

    /**
     * Search words by keyword
     */
    fun searchWords(keyword: String) {
        Log.d(TAG, "=== searchWords() called with keyword: $keyword ===")
        if (keyword.isBlank()) {
            loadWords()
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                Log.d(TAG, "Calling API: searchWords(keyword=$keyword)")
                RetrofitClient.getApiService().searchWords(
                    keyword = keyword,
                    page = 0,
                    size = 100
                ).enqueue(object : Callback<WordListResponse> {
                    override fun onResponse(
                        call: Call<WordListResponse>,
                        response: Response<WordListResponse>
                    ) {
                        Log.d(TAG, "searchWords onResponse: isSuccessful=${response.isSuccessful}")
                        _isLoading.value = false

                        if (response.isSuccessful) {
                            val apiResponse = response.body()
                            Log.d(TAG, "searchWords apiResponse: $apiResponse")
                            if (apiResponse?.isSuccess == true && apiResponse.data != null) {
                                _words.value = apiResponse.data
                                _uiState.value = UiState.Success
                                Log.d(TAG, "searchWords SUCCESS: ${apiResponse.data.size} words")
                            } else {
                                _errorMessage.value = apiResponse?.message ?: "Search failed"
                                _uiState.value = UiState.Error(_errorMessage.value!!)
                                Log.e(TAG, "searchWords ERROR: ${_errorMessage.value}")
                            }
                        } else {
                            _errorMessage.value = "Server error: ${response.code()}"
                            _uiState.value = UiState.Error(_errorMessage.value!!)
                            Log.e(TAG, "searchWords HTTP ERROR: ${response.code()}")
                        }
                    }

                    override fun onFailure(call: Call<WordListResponse>, t: Throwable) {
                        Log.e(TAG, "searchWords onFailure: ${t.message}")
                        _isLoading.value = false
                        _errorMessage.value = "Network error: ${t.message}"
                        _uiState.value = UiState.Error(_errorMessage.value!!)
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "searchWords Exception: ${e.message}")
                _isLoading.value = false
                _errorMessage.value = "Error: ${e.message}"
                _uiState.value = UiState.Error(_errorMessage.value!!)
            }
        }
    }

    /**
     * Filter words by status
     */
    fun filterByStatus(status: String?) {
        Log.d(TAG, "=== filterByStatus() called with status: $status ===")
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                Log.d(TAG, "Calling API: getUserWords(status=$status)")
                RetrofitClient.getApiService().getUserWords(
                    status = status,
                    page = 0,
                    size = 100
                ).enqueue(object : Callback<WordListResponse> {
                    override fun onResponse(
                        call: Call<WordListResponse>,
                        response: Response<WordListResponse>
                    ) {
                        Log.d(TAG, "filterByStatus onResponse: isSuccessful=${response.isSuccessful}")
                        _isLoading.value = false

                        if (response.isSuccessful) {
                            val apiResponse = response.body()
                            Log.d(TAG, "filterByStatus apiResponse: $apiResponse")
                            if (apiResponse?.isSuccess == true && apiResponse.data != null) {
                                _words.value = apiResponse.data
                                _uiState.value = UiState.Success
                                Log.d(TAG, "filterByStatus SUCCESS: ${apiResponse.data.size} words")
                            } else {
                                _errorMessage.value = apiResponse?.message ?: "Filter failed"
                                _uiState.value = UiState.Error(_errorMessage.value!!)
                                Log.e(TAG, "filterByStatus ERROR: ${_errorMessage.value}")
                            }
                        } else {
                            _errorMessage.value = "Server error: ${response.code()}"
                            _uiState.value = UiState.Error(_errorMessage.value!!)
                            Log.e(TAG, "filterByStatus HTTP ERROR: ${response.code()}")
                        }
                    }

                    override fun onFailure(call: Call<WordListResponse>, t: Throwable) {
                        Log.e(TAG, "filterByStatus onFailure: ${t.message}")
                        _isLoading.value = false
                        _errorMessage.value = "Network error: ${t.message}"
                        _uiState.value = UiState.Error(_errorMessage.value!!)
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "filterByStatus Exception: ${e.message}")
                _isLoading.value = false
                _errorMessage.value = "Error: ${e.message}"
                _uiState.value = UiState.Error(_errorMessage.value!!)
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}
