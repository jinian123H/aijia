package com.aijia.video.data.repository

import com.aijia.video.data.model.ApiResponse
import com.aijia.video.data.model.CheckVersionData
import com.aijia.video.data.model.CheckVersionRequest
import com.aijia.video.data.model.CheckForceUpdateRequest
import com.aijia.video.data.model.CheckForceUpdateResponse
import com.aijia.video.data.remote.ApiService
import com.aijia.video.util.NetworkErrorHandler
import com.aijia.video.util.VersionUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 版本更新仓库
 * 新架构：POST /api/v1/app/check-version
 * Redis 永久缓存 + update_version_id 本地比对
 */
@Singleton
class VersionRepository @Inject constructor(
    private val apiService: ApiService
) {
    private val gson = Gson()

    /**
     * 检查版本更新
     * - 上传本地 version_code + version_name + update_version_id
     * - 后端返回 update_version_id + need_update + force_update/optional_update
     */
    suspend fun checkAppVersion(
        context: android.content.Context,
        currentUpdateVersionId: String
    ): Result<CheckVersionData> {
        return try {
            val versionCode = VersionUtils.getVersionCode(context)
            val versionName = VersionUtils.getVersionName(context)

            val request = CheckVersionRequest(
                versionCode = versionCode,
                versionName = versionName,
                updateVersionId = currentUpdateVersionId
            )

            val response = apiService.checkAppVersion(request)
            if (response.isSuccessful) {
                val json = response.body()?.string() ?: return Result.failure(Exception("响应为空"))
                val type = object : TypeToken<ApiResponse<CheckVersionData>>() {}.type
                val body: ApiResponse<CheckVersionData> = gson.fromJson(json, type)
                if (body.code == 1 || body.code == 200) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body.msg))
                }
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(NetworkErrorHandler.formatError(e))
        }
    }

    /**
     * 强制更新检查
     * - 上传本地 version_code + version_name + update_version_id
     * - 后端返回是否需要强制更新
     */
    suspend fun checkForceUpdate(
        context: android.content.Context,
        currentUpdateVersionId: String
    ): Result<CheckForceUpdateResponse> {
        return try {
            val versionCode = VersionUtils.getVersionCode(context)
            val versionName = VersionUtils.getVersionName(context)

            val request = CheckForceUpdateRequest(
                versionCode = versionCode,
                versionName = versionName,
                updateVersionId = currentUpdateVersionId,
                platform = "android"
            )

            val response = apiService.checkForceUpdate(request)
            if (response.isSuccessful) {
                val json = response.body()?.string() ?: return Result.failure(Exception("响应为空"))
                val type = object : TypeToken<ApiResponse<CheckForceUpdateResponse>>() {}.type
                val body: ApiResponse<CheckForceUpdateResponse> = gson.fromJson(json, type)
                if (body.code == 1 || body.code == 200) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body.msg))
                }
            } else if (response.code() == 404) {
                // 接口不存在，不做强制更新
                Result.success(CheckForceUpdateResponse(needUpdate = false, forceUpdate = false, content = null, updateVersionId = ""))
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(NetworkErrorHandler.formatError(e))
        }
    }
}
