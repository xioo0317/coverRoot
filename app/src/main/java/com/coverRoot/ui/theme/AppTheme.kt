package com.coverRoot.ui.theme

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.coverRoot.data.AppPreferences
import com.coverRoot.ui.LocalUiMode
import com.coverRoot.ui.UiMode
import com.coverRoot.util.LocalBlurAlpha
import com.coverRoot.util.LocalBlurIntensity

@Composable
fun AppTheme(
    appPreferences: AppPreferences,
    uiMode: UiMode = LocalUiMode.current,
    content: @Composable () -> Unit,
) {
    val settings = ThemeController.from(appPreferences)
    val colorMode = settings.colorMode
    val isDark = colorMode.isDark || (colorMode.isSystem && isSystemDark())

    val blurSupported = appPreferences.enableBlur &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val glassSupported =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    CompositionLocalProvider(
        LocalColorMode provides colorMode.value,
        LocalEnableBlur provides blurSupported,
        LocalEnableFloatingBottomBar provides appPreferences.enableFloatingBottomBar,
        LocalEnableFloatingBottomBarBlur provides (glassSupported && appPreferences.enableFloatingBottomBarBlur),
        LocalMiuixMonet provides appPreferences.miuixMonet,
        LocalBlurIntensity provides appPreferences.blurIntensity,
        LocalBlurAlpha provides appPreferences.blurAlpha,
    ) {
        when (uiMode) {
            UiMode.Miuix -> MiuixAppTheme(
                isDark = isDark,
                miuixMonet = appPreferences.miuixMonet,
                keyColor = settings.keyColor,
                paletteStyle = settings.paletteStyle,
                colorSpec = settings.colorSpec,
                content = content,
            )

            UiMode.Material -> MaterialAppTheme(
                isDark = isDark,
                isMonet = colorMode.isMonet,
                keyColor = settings.keyColor,
                paletteStyle = settings.paletteStyle,
                colorSpec = settings.colorSpec.effectiveFor(settings.paletteStyle),
                isAmoled = colorMode.isAmoled,
                content = content,
            )
        }
    }
}

@Composable
private fun isSystemDark() = androidx.compose.foundation.isSystemInDarkTheme()
