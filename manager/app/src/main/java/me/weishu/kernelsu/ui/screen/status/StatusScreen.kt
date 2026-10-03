package me.weishu.kernelsu.ui.screen.status

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.dropUnlessResumed
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.navigation3.LocalNavigator

@Composable
fun StatusScreen() {
    val navigator = LocalNavigator.current
    val onBack = dropUnlessResumed { navigator.pop() }

    when (LocalUiMode.current) {
        UiMode.Miuix -> StatusScreenMiuix(onBack)
        UiMode.Material -> StatusScreenMaterial(onBack)
    }
}
