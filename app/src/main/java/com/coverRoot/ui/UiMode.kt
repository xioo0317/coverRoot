package com.coverRoot.ui

import androidx.compose.runtime.staticCompositionLocalOf

enum class UiMode(val value: String) {
    Miuix("miuix"),
    Material("material");

    companion object {
        fun fromValue(value: String): UiMode = when (value) {
            Miuix.value -> Miuix
            else -> Material
        }

        val DEFAULT_VALUE: String = Miuix.value
    }
}

val LocalUiMode = staticCompositionLocalOf { UiMode.Material }
