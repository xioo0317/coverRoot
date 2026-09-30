package com.coverRoot.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.coverRoot.BuildConfig
import com.coverRoot.R
import com.coverRoot.data.AppPreferences
import com.coverRoot.data.LocaleHelper
import com.coverRoot.data.SettingsRepository
import com.coverRoot.data.UpdateChecker
import com.coverRoot.data.LocalApiService
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import com.coverRoot.ui.component.material.ExpressiveScaffold
import com.coverRoot.ui.component.material.SegmentedColumn
import com.coverRoot.ui.component.material.SegmentedDropdownItem
import com.coverRoot.ui.component.material.SegmentedListItem
import com.coverRoot.ui.component.material.expressiveTopAppBarColors
import kotlinx.coroutines.launch

@Composable
fun SettingsMaterialScreen(
    onOpenAppearance: () -> Unit,
    onOpenServerAddress: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val preferences by repository.preferencesFlow.collectAsState(initial = AppPreferences())
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // 更新检查
    val updateChecker = remember { UpdateChecker(context) }
    val updateInfoState = remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    val showUpdateDialog = remember { mutableStateOf(false) }
    val checkingUpdate = remember { mutableStateOf(false) }
    val downloadState = remember { mutableStateOf(MaterialDownloadState()) }

    val uiModeOptions = listOf(
        stringResource(R.string.mode_miuix),
        stringResource(R.string.mode_material),
    )
    val languageOptions = LocaleHelper.supportedLanguages.map {
        if (it.code == "system") stringResource(R.string.lang_system) else it.nativeName
    }

    // KSU 同款：原生 M3 Expressive Scaffold + 大标题顶栏 + SegmentedColumn 分组列表
    ExpressiveScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.nav_settings)) },
                colors = expressiveTopAppBarColors(),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding(),
                ),
        ) {
            // 外观组：界面模式（内联下拉）+ 主题入口
            SegmentedColumn(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 13.dp),
            ) {
                item {
                    SegmentedDropdownItem(
                        icon = Icons.Filled.Style,
                        title = stringResource(R.string.settings_ui_mode),
                        summary = stringResource(R.string.settings_ui_mode_summary),
                        items = uiModeOptions,
                        selectedIndex = if (preferences.uiMode == "miuix") 0 else 1,
                        onItemSelected = { index ->
                            scope.launch {
                                repository.setUiMode(if (index == 0) "miuix" else "material")
                            }
                        },
                    )
                }
                item {
                    SegmentedListItem(
                        onClick = onOpenAppearance,
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.Colorize,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        headlineContent = { Text(stringResource(R.string.settings_theme)) },
                        supportingContent = { Text(stringResource(R.string.settings_theme_summary)) },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
            }

            // 通用组：语言（内联下拉）
            SegmentedColumn(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 13.dp),
            ) {
                item {
                    SegmentedDropdownItem(
                        icon = Icons.Filled.Language,
                        title = stringResource(R.string.settings_language),
                        summary = stringResource(R.string.settings_language_summary),
                        items = languageOptions,
                        selectedIndex = LocaleHelper.supportedLanguages
                            .indexOfFirst { it.code == preferences.language }
                            .coerceIn(0, languageOptions.lastIndex),
                        onItemSelected = { index ->
                            val lang = LocaleHelper.supportedLanguages.getOrNull(index)
                            if (lang != null && lang.code != preferences.language) {
                                scope.launch { repository.setLanguage(lang.code) }
                            }
                        },
                    )
                }
            }

            // 更新 + 后端地址组
            SegmentedColumn(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 13.dp),
            ) {
                item {
                    SegmentedListItem(
                        onClick = {
                            if (!checkingUpdate.value) {
                                scope.launch {
                                    checkingUpdate.value = true
                                    val info = updateChecker.checkForUpdate()
                                    updateInfoState.value = info
                                    checkingUpdate.value = false
                                    if (info != null) showUpdateDialog.value = true
                                }
                            }
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Rounded.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        headlineContent = { Text(stringResource(R.string.settings_update)) },
                        supportingContent = {
                            Text(
                                if (checkingUpdate.value) "检查中…"
                                else if (updateInfoState.value != null) stringResource(R.string.update_available)
                                else stringResource(R.string.settings_update_summary)
                            )
                        },
                        trailingContent = {
                            if (updateInfoState.value != null) {
                                androidx.compose.material3.FilledTonalButton(
                                    onClick = { showUpdateDialog.value = true }
                                ) { Text(stringResource(R.string.update_download)) }
                            }
                        },
                    )
                }
                item {
                    SegmentedListItem(
                        onClick = onOpenServerAddress,
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Rounded.VpnKey,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        headlineContent = { Text(stringResource(R.string.nav_server_address)) },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
            }
            // 隐藏图标组
            SegmentedColumn(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 13.dp),
            ) {
                item {
                    SegmentedListItem(
                        headlineContent = { Text(stringResource(R.string.settings_hide_icon)) },
                        supportingContent = { Text(stringResource(R.string.settings_hide_icon_summary)) },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Rounded.VpnKey,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        trailingContent = {
                            androidx.compose.material3.Switch(
                                checked = preferences.hideAppIcon,
                                onCheckedChange = { checked ->
                                    scope.launch {
                                        repository.setHideAppIcon(checked)
                                        val api = LocalApiService(context)
                                        api.execute(if (checked) "tool_hide_icon" else "tool_show_icon")
                                    }
                                },
                            )
                        },
                    )
                }
            }
            // 关于组：版本号 + 入口
            SegmentedColumn(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 13.dp),
            ) {
                item {
                    SegmentedListItem(
                        onClick = onOpenAbout,
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        headlineContent = { Text(stringResource(R.string.nav_about)) },
                        supportingContent = { Text("v" + BuildConfig.VERSION_NAME) },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
            }
        }
    }

    // 更新对话框（Material 版，支持直接下载安装）
    if (showUpdateDialog.value && updateInfoState.value != null) {
        val info = updateInfoState.value!!
        MaterialUpdateDialog(
            info = info,
            downloadState = downloadState.value,
            onDismiss = {
                if (downloadState.value.status != MaterialUpdateStatus.DOWNLOADING) {
                    showUpdateDialog.value = false
                }
            },
            onDownload = {
                if (downloadState.value.status == MaterialUpdateStatus.DOWNLOADING) return@MaterialUpdateDialog
                scope.launch {
                    downloadState.value = MaterialDownloadState(MaterialUpdateStatus.DOWNLOADING)
                    val file = com.coverRoot.util.ApkInstaller.download(
                        context = context,
                        url = info.zipUrl,
                    ) { p ->
                        downloadState.value =
                            MaterialDownloadState(MaterialUpdateStatus.DOWNLOADING, p)
                    }
                    downloadState.value = if (file != null) {
                        MaterialDownloadState(MaterialUpdateStatus.READY, file = file)
                    } else {
                        MaterialDownloadState(MaterialUpdateStatus.FAILED)
                    }
                }
            },
            onInstall = {
                val file = downloadState.value.file ?: return@MaterialUpdateDialog
                com.coverRoot.util.ApkInstaller.install(context, file)
            },
        )
    }
}

@Composable
private fun MaterialUpdateDialog(
    info: UpdateChecker.UpdateInfo,
    downloadState: MaterialDownloadState,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
) {
    val downloading = downloadState.status == MaterialUpdateStatus.DOWNLOADING
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { if (!downloading) onDismiss() },
        title = { Text(stringResource(R.string.update_available)) },
        text = {
            Column {
                Text("v${info.version} (${info.versionCode})")
                if (!info.changelog.isNullOrEmpty()) {
                    androidx.compose.foundation.layout.Spacer(
                        Modifier.height(8.dp)
                    )
                    Text(
                        text = info.changelog,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                when (downloadState.status) {
                    MaterialUpdateStatus.READY -> onInstall()
                    MaterialUpdateStatus.FAILED, MaterialUpdateStatus.IDLE -> onDownload()
                    MaterialUpdateStatus.DOWNLOADING -> {}
                }
            }) {
                Text(
                    when (downloadState.status) {
                        MaterialUpdateStatus.DOWNLOADING ->
                            stringResource(R.string.update_downloading, downloadState.progress)
                        MaterialUpdateStatus.READY -> stringResource(R.string.update_install)
                        MaterialUpdateStatus.FAILED -> stringResource(R.string.update_download_failed)
                        MaterialUpdateStatus.IDLE -> stringResource(R.string.update_download)
                    }
                )
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(
                onClick = { if (!downloading) onDismiss() }
            ) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

private enum class MaterialUpdateStatus { IDLE, DOWNLOADING, READY, FAILED }

private data class MaterialDownloadState(
    val status: MaterialUpdateStatus = MaterialUpdateStatus.IDLE,
    val progress: Int = 0,
    val file: java.io.File? = null,
)
