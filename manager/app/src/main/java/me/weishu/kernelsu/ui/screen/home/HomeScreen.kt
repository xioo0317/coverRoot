package me.weishu.kernelsu.ui.screen.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.navigation3.Navigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.viewmodel.HomeViewModel

@Composable
fun HomePager(
    navigator: Navigator,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean = true,
) {
    val viewModel = viewModel<HomeViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage) {
            viewModel.refresh()
        }
    }

    val onOpenStatusPage = dropUnlessResumed { navigator.push(Route.StatusPage) }

    val actions = HomeActions(
        onInstallClick = { /* Install flow is not available in this UI-only shell. */ },
        onOpenUrl = { url -> uriHandler.openUri(url) },
        onOpenStatusPage = onOpenStatusPage,
    )

    when (LocalUiMode.current) {
        UiMode.Material -> HomePagerMaterial(uiState, actions, bottomInnerPadding)
        UiMode.Miuix -> HomePagerMiuix(uiState, actions, bottomInnerPadding)
    }
}
