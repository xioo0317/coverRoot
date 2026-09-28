package com.coverRoot.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Checks for app updates and reads their changelog.
 *
 * Refactored from a singleton to a context-aware class so screens can create a
 * per-screen instance (and cache the latest [UpdateInfo]) without keeping
 * process-global state.
 */
class UpdateChecker(private val context: Context) {

    @Serializable
    data class UpdateInfo(
        val version: String = "",
        val versionCode: Int = 0,
        val zipUrl: String = "",
        val changelog: String = "",
    )

    private var latest: UpdateInfo? = null

    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        val info = try {
            val request = Request.Builder().url(UPDATE_JSON_URL).build()
            val response = jsonClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext null
                Json { ignoreUnknownKeys = true }.decodeFromString(UpdateInfo.serializer(), body)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
        latest = info
        info
    }

    suspend fun fetchChangelog(): String = latest?.changelog ?: ""

    suspend fun fetchChangelog(url: String): String = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = jsonClient.newCall(request).execute()
            if (response.isSuccessful) response.body?.string() ?: "" else ""
        } catch (e: Exception) {
            ""
        }
    }

    private companion object {
        const val UPDATE_JSON_URL =
            "https://raw.githubusercontent.com/xioo0317/coverRoot/main/update.json"

        val jsonClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }
}
