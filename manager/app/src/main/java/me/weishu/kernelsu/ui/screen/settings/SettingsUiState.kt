package me.weishu.kernelsu.ui.screen.settings

import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec

data class SettingsUiState(
    val uiMode: String = "material",
    val themeMode: Int = 0,
    val miuixMonet: Boolean = false,
    val keyColor: Int = 0,
    val colorStyle: String = PaletteStyle.TonalSpot.name,
    val colorSpec: String = ColorSpec.SpecVersion.SPEC_2025.name,
    val enablePredictiveBack: Boolean = false,
    val enableBlur: Boolean = false,
    val enableFloatingBottomBar: Boolean = false,
    val enableFloatingBottomBarBlur: Boolean = false,
    val pageScale: Float = 1.0f,
    val checkUpdate: Boolean = true,
    val appLanguage: String = "system",
)

data class SettingsScreenActions(
    val onOpenTheme: () -> Unit = {},
    val onSetUiModeIndex: (Int) -> Unit = {},
    val onOpenAbout: () -> Unit = {},
    val onSetCheckUpdate: (Boolean) -> Unit = {},
    val onSetAppLanguage: (String) -> Unit = {},
)
