package com.coverRoot.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object LocalApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun sendAction(
        baseUrl: String,
        action: String,
        params: Map<String, String> = emptyMap(),
    ): String? = withContext(Dispatchers.IO) {
        val paramsJson = params.entries.joinToString(",") { "\"${it.key}\":\"${it.value}\"" }
        val body = """{"action":"$action","params":{$paramsJson}}"""
        val url = "${baseUrl.trimEnd('/')}/api/v1/execute"
        try {
            val request = Request.Builder()
                .url(url)
                .post(body.toRequestBody(jsonMedia))
                .build()
            val response = client.newCall(request).execute()
            response.body?.string()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun hideAppIcon(baseUrl: String) =
        sendAction(baseUrl, "tool_hide_icon", mapOf("package" to "com.coverRoot"))

    suspend fun showAppIcon(baseUrl: String) =
        sendAction(baseUrl, "tool_show_icon", mapOf("package" to "com.coverRoot"))

    suspend fun detect(baseUrl: String) = sendAction(baseUrl, "detect")
    suspend fun version(baseUrl: String) = sendAction(baseUrl, "version")
    suspend fun debug(baseUrl: String) = sendAction(baseUrl, "debug")
    suspend fun sysinfo(baseUrl: String) = sendAction(baseUrl, "sysinfo")
    suspend fun modules(baseUrl: String) = sendAction(baseUrl, "modules")
    suspend fun config(baseUrl: String) = sendAction(baseUrl, "config")
    suspend fun susfsSetup(baseUrl: String) = sendAction(baseUrl, "susfs_setup")
    suspend fun hideAppList(baseUrl: String) = sendAction(baseUrl, "hide_app_list")
    suspend fun updateKey(baseUrl: String) = sendAction(baseUrl, "update_key")
    suspend fun setHash(baseUrl: String) = sendAction(baseUrl, "set_hash")
}
