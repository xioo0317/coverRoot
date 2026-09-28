package com.coverRoot.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

@Composable
fun MiuixAppTheme(
    isDark: Boolean,
    miuixMonet: Boolean = false,
    keyColor: Int = 0,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2025,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val miuixPaletteStyle = try {
        ThemePaletteStyle.valueOf(paletteStyle.name)
    } catch (_: Exception) {
        ThemePaletteStyle.TonalSpot
    }
    val miuixSpec = if (colorSpec == ColorSpec.SpecVersion.SPEC_2025) {
        ThemeColorSpec.Spec2025
    } else {
        ThemeColorSpec.Spec2021
    }

    val resolvedKeyColor: Color? = when {
        !miuixMonet -> null
        keyColor != 0 -> Color(keyColor)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (isDark) dynamicDarkColorScheme(context).primary
            else dynamicLightColorScheme(context).primary
        else -> null
    }
    val useMonet = resolvedKeyColor != null

    val controller = ThemeController(
        if (useMonet) {
            if (isDark) ColorSchemeMode.MonetDark else ColorSchemeMode.MonetLight
        } else {
            if (isDark) ColorSchemeMode.Dark else ColorSchemeMode.Light
        },
        keyColor = resolvedKeyColor,
        isDark = isDark,
        paletteStyle = miuixPaletteStyle,
        colorSpec = miuixSpec,
    )

    MiuixTheme(controller = controller) {
        LaunchedEffect(isDark) {
            val window = (context as? Activity)?.window ?: return@LaunchedEffect
            WindowInsetsControllerCompat(window, window.decorView).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
        content()
    }
}
