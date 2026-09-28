package com.coverRoot.data

data class AppPreferences(
    val colorMode: Int = 0,
    val uiMode: String = "miuix",
    val language: String = "system",
    val keyColor: Int = 0,
    val colorStyle: String = "TonalSpot",
    val colorSpec: String = "SPEC_2025",
    val miuixMonet: Boolean = false,
    val enableBlur: Boolean = false,
    val enableFloatingBottomBar: Boolean = false,
    val enableFloatingBottomBarBlur: Boolean = false,
    val enableScrollAnimation: Boolean = false,
    val enablePredictiveBack: Boolean = true,
    val enableSwipeDismiss: Boolean = true,
    val pageScale: Float = 1.0f,
    val blurIntensity: Float = 35f,
    val blurAlpha: Float = 0.75f,
    val hideAppIcon: Boolean = false,
)
