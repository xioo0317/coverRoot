package com.coverRoot.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@Serializable
data class UpdateInfo(
    val version: String = "",
    val versionCode: Int = 0,
    val zipUrl: String = "",
    val changelog: String = "",
)

object UpdateChecker {
    private const val UPDATE_JSON_URL =
        "https://raw.githubusercontent.com/xioo0317/coverRoot/main/update.json"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchUpdateInfo(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(UPDATE_JSON_URL).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext null
                json.decodeFromString(UpdateInfo.serializer(), body)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchChangelog(url: String): String = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) response.body?.string() ?: "" else ""
        } catch (e: Exception) {
            ""
        }
    }
}
