const fs = require('fs');
let repo = fs.readFileSync('E:/app/aijia/app/src/main/java/com/aijia/video/data/repository/VideoRepository.kt', 'utf8');

const insertPoint = repo.indexOf('suspend fun getRankIndex');
if (insertPoint < 0) { console.log('FAIL'); process.exit(1); }

const newMethod = `
    /**
     * 获取首页推荐（每日缓存模式）
     * 通过 version_id 判断是否需要更新，本地缓存首页数据
     */
    suspend fun getHomeRecommend(): Result<List<HomeSection>?> {
        return try {
            val versionId = homeCachePrefs.getString(KEY_CACHED_VERSION_ID, "") ?: ""

            android.util.Log.d("VideoRepository", "getHomeRecommend - 发送version_id: $versionId")

            val response = apiService.getHomeIndex(versionId)
            if (response.isSuccessful) {
                val json = response.body()?.string() ?: return Result.failure(Exception("Empty response"))
                val type = object : TypeToken<ApiResponse<HomeRecommendVersionedData>>() {}.type
                val apiResponse: ApiResponse<HomeRecommendVersionedData> = gson.fromJson(json, type)
                if (apiResponse.code == 200) {
                    val versionedData = apiResponse.data

                    if (versionedData.dataChanged && versionedData.content != null) {
                        // 有更新：保存到本地缓存，返回新数据
                        homeCachePrefs.edit()
                            .putString(KEY_CACHED_VERSION_ID, versionedData.versionId)
                            .putString(KEY_CACHED_DATA, gson.toJson(versionedData.content))
                            .apply()
                        android.util.Log.d("VideoRepository", "getHomeRecommend - 数据已更新，version_id: ${versionedData.versionId}")
                        Result.success(versionedData.content)
                    } else {
                        // 无更新：使用本地缓存
                        val cachedJson = homeCachePrefs.getString(KEY_CACHED_DATA, null)
                        if (cachedJson != null) {
                            val type = object : TypeToken<List<HomeSection>>() {}.type
                            val cachedData: List<HomeSection> = gson.fromJson(cachedJson, type)
                            android.util.Log.d("VideoRepository", "getHomeRecommend - 无更新，使用本地缓存")
                            Result.success(cachedData)
                        } else {
                            // 本地无缓存（首次或缓存被清除）
                            // 用空 version_id 重试，强制服务端返回数据
                            if (versionId.isNotEmpty()) {
                                homeCachePrefs.edit().putString(KEY_CACHED_VERSION_ID, "").apply()
                                return getHomeRecommend()
                            }
                            Result.success(emptyList())
                        }
                    }
                } else {
                    Result.failure(Exception(apiResponse.msg))
                }
            } else {
                Result.failure(Exception("Network error: \${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("VideoRepository", "getHomeRecommend异常", e)
            // 网络失败时使用本地缓存兜底
            val cachedJson = homeCachePrefs.getString(KEY_CACHED_DATA, null)
            if (cachedJson != null) {
                val type = object : TypeToken<List<HomeSection>>() {}.type
                val cachedData: List<HomeSection> = gson.fromJson(cachedJson, type)
                Result.success(cachedData)
            } else {
                Result.failure(NetworkErrorHandler.formatError(e))
            }
        }
    }

`;

repo = repo.substring(0, insertPoint) + newMethod + repo.substring(insertPoint);
fs.writeFileSync('E:/app/aijia/app/src/main/java/com/aijia/video/data/repository/VideoRepository.kt', repo, 'utf8');
console.log('OK');
