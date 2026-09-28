package com.coverRoot.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File

class ConfigRepository(context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val configFile: File = File(context.filesDir, FILE_NAME)
    private val writeLock = Mutex()

    private val _config = MutableStateFlow(load())
    val configFlow: StateFlow<AppConfig> = _config.asStateFlow()

    private fun load(): AppConfig = try {
        if (configFile.exists()) {
            json.decodeFromString(AppConfig.serializer(), configFile.readText())
        } else {
            AppConfig().also { persist(it) }
        }
    } catch (e: Exception) {
        AppConfig()
    }

    private fun persist(config: AppConfig) {
        configFile.parentFile?.mkdirs()
        configFile.writeText(json.encodeToString(AppConfig.serializer(), config))
    }

    suspend fun setBackendUrl(url: String) = writeLock.withLock {
        val normalized = url.trim().trimEnd('/')
        if (normalized.isEmpty() || normalized == _config.value.backendUrl) return@withLock
        val updated = _config.value.copy(backendUrl = normalized)
        persist(updated)
        _config.value = updated
    }

    companion object {
        const val FILE_NAME = "config.json"

        suspend fun getBackendUrl(context: Context): String {
            return try {
                val repo = ConfigRepository(context)
                repo.configFlow.value.backendUrl
            } catch (_: Exception) {
                AppConfig.DEFAULT_BACKEND_URL
            }
        }
    }
}
