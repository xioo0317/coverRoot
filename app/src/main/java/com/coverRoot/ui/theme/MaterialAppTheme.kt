package com.coverRoot.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme

private fun amoled(
    base: androidx.compose.material3.ColorScheme,
): androidx.compose.material3.ColorScheme = base.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF050505),
    surfaceContainer = Color(0xFF0A0A0A),
    surfaceContainerHigh = Color(0xFF121212),
    surfaceContainerHighest = Color(0xFF1B1B1B),
    surfaceBright = Color(0xFF1B1B1B),
    surfaceDim = Color.Black,
)

@Composable
fun MaterialAppTheme(
    isDark: Boolean = isSystemInDarkTheme(),
    @Suppress("unused") isMonet: Boolean = true,
    keyColor: Int = 0,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2025,
    isAmoled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val seed = if (keyColor != 0) {
        Color(keyColor)
    } else {
        (if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)).primary
    }
    val base = rememberDynamicColorScheme(
        seedColor = seed,
        isDark = isDark,
        style = paletteStyle,
        specVersion = colorSpec.effectiveFor(paletteStyle),
    )

    val colorScheme = if (isDark && isAmoled) amoled(base) else base

    LaunchedEffect(isDark) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
    }

    val animatedColorScheme = colorScheme.animateAsState()

    MaterialExpressiveTheme(
        colorScheme = animatedColorScheme,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
