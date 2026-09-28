package com.coverRoot.ui.screen

import androidx.compose.runtime.Composable
import com.coverRoot.ui.LocalUiMode
import com.coverRoot.ui.UiMode
import com.coverRoot.ui.screen.appearance.AppearanceMaterial
import com.coverRoot.ui.screen.appearance.AppearanceMiuix

@Composable
fun AppearanceScreen() {
    when (LocalUiMode.current) {
        UiMode.Miuix -> AppearanceMiuix()
        UiMode.Material -> AppearanceMaterial()
    }
}
