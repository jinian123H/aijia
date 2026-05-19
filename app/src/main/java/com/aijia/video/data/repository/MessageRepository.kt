package com.aijia.video.data.repository

import com.aijia.video.data.model.ApiResponse
import com.aijia.video.data.remote.ApiService
import com.aijia.video.util.NetworkErrorHandler
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val apiService: ApiService
) {
    private val gson = Gson()

    suspend fun submitMessage(content: String, contact: String = ""): Result<Unit> {
        return try {
            val response = apiService.submitMessage(content, contact)
            if (response.isSuccessful) {
                val json = response.body()?.string() ?: return Result.failure(Exception("Empty response"))
                val apiResponse: ApiResponse<*> = gson.fromJson(json, ApiResponse::class.java)
                if (apiResponse.code == 1 || apiResponse.code == 200) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(apiResponse.msg))
                }
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(NetworkErrorHandler.formatError(e))
        }
    }
}
