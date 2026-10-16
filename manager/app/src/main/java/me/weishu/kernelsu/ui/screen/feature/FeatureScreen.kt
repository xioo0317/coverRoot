package me.weishu.kernelsu.ui.screen.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.util.BlurredBar
import me.weishu.kernelsu.ui.util.rememberBlurBackdrop
import me.weishu.kernelsu.ui.viewmodel.FeatureViewModel
import android.widget.Toast
import kotlinx.coroutines.flow.collectLatest

/**
 * 功能页入口（取代原“模块页”）：
 * 展示后端 (local_api) 提供的功能列表，点击执行并查看结果，
 * 用于验证前后端通讯。
 */
@Composable
fun FeaturePager(
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean = true,
) {
    val viewModel = viewModel<FeatureViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage) viewModel.initialize()
    }
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is FeatureViewModel.Effect.ShowToast ->
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    when (LocalUiMode.current) {
        UiMode.Material -> FeaturePagerMaterial(uiState, viewModel, bottomInnerPadding)
        UiMode.Miuix -> FeaturePagerMiuix(uiState, viewModel, bottomInnerPadding)
    }
}

internal fun formatBackendSummary(state: FeatureViewModel.UiState): String {
    return when (state.backendOnline) {
        true -> "后端在线 · ${state.backendVersion ?: "unknown"} (API ${state.apiVersion ?: "?"})"
        false -> "后端不可达"
        null -> "检测中…"
    }
}
