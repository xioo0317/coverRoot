package me.weishu.kernelsu.ui.screen.module

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.component.SearchStatus
import me.weishu.kernelsu.ui.viewmodel.ModuleViewModel

@Composable
fun ModulePager(
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean = true,
) {
    val uiMode = LocalUiMode.current
    val viewModel = viewModel<ModuleViewModel>()
    val rawUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val latestIsCurrentPage by rememberUpdatedState(isCurrentPage)
    val initialResumeHandled = rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage) {
            if (!initialResumeHandled.value) {
                initialResumeHandled.value = true
                viewModel.refreshEnvironmentState()
                viewModel.initializePreferences()
                val state = viewModel.uiState.value
                if (!state.hasLoaded && !state.isRefreshing) {
                    viewModel.fetchModuleList()
                }
            }
        } else if (!rawUiState.searchStatus.isCollapsed()) {
            viewModel.updateSearchStatus(rawUiState.searchStatus.copy(searchText = "", current = SearchStatus.Status.COLLAPSED))
        }
    }

    LifecycleResumeEffect(Unit) {
        if (initialResumeHandled.value && latestIsCurrentPage) {
            val state = viewModel.uiState.value
            if (!state.isRefreshing) {
                viewModel.fetchModuleList(
                    checkUpdate = !state.hasLoaded || viewModel.isNeedRefresh,
                    resort = !state.hasLoaded,
                )
            }
        }
        initialResumeHandled.value = true
        onPauseOrDispose {}
    }

    val actions = ModuleActions(
        onRefresh = {
            viewModel.fetchModuleList(checkUpdate = true)
        },
        onSearchStatusChange = {
            viewModel.updateSearchStatus(it)
        },
        onSearchTextChange = { text ->
            viewModel.updateSearchText(text)
        },
        onClearSearch = {
            viewModel.updateSearchText("")
        },
        onRequestUpdateConfirmation = { module, updateInfo ->
            viewModel.requestUpdateConfirmation(module, updateInfo)
        },
        onRequestUninstallConfirmation = { module ->
            viewModel.requestUninstallConfirmation(module)
        },
        onDismissConfirmRequest = {
            viewModel.dismissConfirmRequest()
        },
        onConfirmUpdate = { _ ->
            // Downloads are not available in this UI-only shell.
            viewModel.dismissConfirmRequest()
        },
        onOpenRepo = { /* Module repo is not available in this UI-only shell. */ },
        onToggleSortActionFirst = {
            viewModel.toggleSortActionFirst()
        },
        onToggleSortEnabledFirst = {
            viewModel.toggleSortEnabledFirst()
        },
        onOpenWebUi = { _ -> /* WebUI is not available in this UI-only shell. */ },
        onToggleModule = { module ->
            viewModel.toggleModule(module)
        },
        onUninstallModule = { module ->
            viewModel.uninstallModule(module)
        },
        onUndoUninstallModule = { module ->
            viewModel.undoUninstallModule(module)
        },
        onOpenFlash = { _ -> /* Flash flow is not available in this UI-only shell. */ },
        onExecuteModuleAction = { _ -> /* Module actions are not available in this UI-only shell. */ },
    )

    when (uiMode) {
        UiMode.Miuix -> ModulePagerMiuix(
            uiState = rawUiState,
            confirmDialogState = rawUiState.confirmDialogState,
            moduleEvent = viewModel.moduleEvent,
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
        )

        UiMode.Material -> ModulePagerMaterial(
            uiState = rawUiState,
            confirmDialogState = rawUiState.confirmDialogState,
            moduleEvent = viewModel.moduleEvent,
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
        )
    }
}
