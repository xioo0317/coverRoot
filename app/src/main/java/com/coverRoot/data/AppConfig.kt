package com.coverRoot.data

import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val backendUrl: String = DEFAULT_BACKEND_URL,
) {
    companion object {
        const val DEFAULT_BACKEND_URL = "http://127.0.0.1:8080"
    }
}
