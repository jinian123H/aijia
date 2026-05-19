package com.aijia.video.util

import android.content.Context
import android.util.Log
import com.aijia.video.data.model.Danmu
import com.aijia.video.data.model.DanmuType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object DanmuStorageManager {
    private const val SP_NAME = "danmu_storage"
    private const val KEY_DANMU_LIST = "danmu_list"

    fun saveDanmu(context: Context, videoKey: String, danmu: Danmu) {
        val danmuList = loadDanmuList(context, videoKey)
        val newDanmuList = danmuList + danmu
        saveDanmuList(context, videoKey, newDanmuList)
        Log.d("DanmuStorage", "Saved danmu: ${danmu.content} for video: $videoKey")
    }

    fun loadDanmuList(context: Context, videoKey: String): List<Danmu> {
        val sharedPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        val danmuJson = sharedPreferences.getString("${KEY_DANMU_LIST}_$videoKey", "[]")
        return try {
            val type = object : TypeToken<List<Danmu>>() {}.type
            Gson().fromJson(danmuJson, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e("DanmuStorage", "Failed to load danmu list for video $videoKey", e)
            emptyList()
        }
    }

    private fun saveDanmuList(context: Context, videoKey: String, danmuList: List<Danmu>) {
        val sharedPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putString("${KEY_DANMU_LIST}_$videoKey", Gson().toJson(danmuList))
            apply()
        }
        Log.d("DanmuStorage", "Saved ${danmuList.size} danmu items for video: $videoKey")
    }

    fun clearAllDanmu(context: Context) {
        val sharedPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
        Log.d("DanmuStorage", "Cleared all danmu data")
    }
}
