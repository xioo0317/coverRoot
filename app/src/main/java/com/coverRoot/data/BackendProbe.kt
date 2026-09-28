package com.coverRoot.data

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class ProbeResult(
    val online: Boolean,
    val latencyMs: Long,
)

suspend fun probeBackend(serverUrl: String): ProbeResult = withContext(Dispatchers.IO) {
    val normalized = serverUrl.trim().trimEnd('/')
    val start = SystemClock.elapsedRealtime()
    var connection: HttpURLConnection? = null
    try {
        connection = (URL(normalized).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 2500
            readTimeout = 2500
            instanceFollowRedirects = false
        }
        val code = connection.responseCode
        ProbeResult(online = code in 200..599, latencyMs = SystemClock.elapsedRealtime() - start)
    } catch (e: IOException) {
        ProbeResult(online = false, latencyMs = SystemClock.elapsedRealtime() - start)
    } finally {
        connection?.disconnect()
    }
}

object BackendMonitor {
    var lastResult by mutableStateOf<ProbeResult?>(null)
        private set
    var lastProbedUrl by mutableStateOf<String?>(null)
        private set

    suspend fun refresh(serverUrl: String): ProbeResult {
        val result = probeBackend(serverUrl)
        lastResult = result
        lastProbedUrl = serverUrl
        return result
    }
}
