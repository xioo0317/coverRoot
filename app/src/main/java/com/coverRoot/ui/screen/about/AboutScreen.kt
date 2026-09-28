package com.coverRoot.ui.screen.about

import androidx.compose.runtime.Composable
import com.coverRoot.ui.LocalUiMode
import com.coverRoot.ui.UiMode
import com.coverRoot.ui.navigation.LocalNavigator
import com.coverRoot.ui.screen.AboutMaterialScreen
import com.coverRoot.ui.screen.AboutMiuixScreen

@Composable
fun AboutScreen() {
    val navigator = LocalNavigator.current
    val onBack = { navigator.pop() }
    when (LocalUiMode.current) {
        UiMode.Miuix -> AboutMiuixScreen(onBack = onBack)
        UiMode.Material -> AboutMaterialScreen(onBack = onBack)
    }
}
