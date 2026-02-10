package com.englishword.ui.screens

import android.content.Context
import android.util.Log
import com.englishword.data.TokenManager
import com.englishword.data.RetrofitClient
import com.englishword.data.model.LoginRequest
import com.englishword.data.model.LoginResponse
import com.englishword.data.model.User
import com.englishword.data.model.ApiResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginViewModel(private val context: Context) {

    private val tokenManager = TokenManager.getInstance(context)
    private val apiService = RetrofitClient.getApiService()

    fun login(username: String, password: String, callback: (Boolean, String?) -> Unit) {
        Log.d("LoginViewModel", "Attempting login for user: $username")
        val request = LoginRequest(username, password)
        apiService.login(request).enqueue(object : Callback<ApiResponse<LoginResponse>> {
            override fun onResponse(
                call: Call<ApiResponse<LoginResponse>>,
                response: Response<ApiResponse<LoginResponse>>
            ) {
                Log.d("LoginViewModel", "Login response code: ${response.code()}")
                Log.d("LoginViewModel", "Login response body: ${response.body()}")

                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (apiResponse.isSuccess && apiResponse.data != null) {
                        val loginResponse = apiResponse.data
                        Log.d("LoginViewModel", "Login successful, token: ${loginResponse.token}")
                        // Save token and user info using coroutine
                        CoroutineScope(Dispatchers.IO).launch {
                            tokenManager.saveToken(loginResponse.token!!)
                            tokenManager.saveUserInfo(
                                loginResponse.userId!!,
                                loginResponse.username!!
                            )
                        }
                        callback(true, null)
                    } else {
                        Log.e("LoginViewModel", "Login failed: ${apiResponse.message}")
                        callback(false, apiResponse.message)
                    }
                } else {
                    Log.e("LoginViewModel", "Login HTTP error: ${response.code()}")
                    callback(false, "Login failed: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse<LoginResponse>>, t: Throwable) {
                Log.e("LoginViewModel", "Login network error", t)
                callback(false, "Network error: ${t.message}")
            }
        })
    }

    fun register(username: String, password: String, callback: (Boolean, String?) -> Unit) {
        Log.d("LoginViewModel", "Attempting registration for user: $username")
        val user = User(null, username, password)
        apiService.register(user).enqueue(object : Callback<ApiResponse<LoginResponse>> {
            override fun onResponse(
                call: Call<ApiResponse<LoginResponse>>,
                response: Response<ApiResponse<LoginResponse>>
            ) {
                Log.d("LoginViewModel", "Register response code: ${response.code()}")
                Log.d("LoginViewModel", "Register response body: ${response.body()}")

                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (apiResponse.isSuccess && apiResponse.data != null) {
                        val loginResponse = apiResponse.data
                        Log.d("LoginViewModel", "Registration successful, token: ${loginResponse.token}")
                        // Save token and user info using coroutine
                        CoroutineScope(Dispatchers.IO).launch {
                            tokenManager.saveToken(loginResponse.token!!)
                            tokenManager.saveUserInfo(
                                loginResponse.userId!!,
                                loginResponse.username!!
                            )
                        }
                        callback(true, null)
                    } else {
                        Log.e("LoginViewModel", "Registration failed: ${apiResponse.message}")
                        callback(false, apiResponse.message)
                    }
                } else {
                    Log.e("LoginViewModel", "Registration HTTP error: ${response.code()}")
                    callback(false, "Registration failed: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse<LoginResponse>>, t: Throwable) {
                Log.e("LoginViewModel", "Registration network error", t)
                callback(false, "Network error: ${t.message}")
            }
        })
    }
}
