package com.coverRoot.ui.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DisplaySettings
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.coverRoot.R
import com.coverRoot.data.AppPreferences
import com.coverRoot.data.LocalApiService
import com.coverRoot.data.SettingsRepository
import com.coverRoot.data.UpdateChecker
import com.coverRoot.ui.LocalUiMode
import com.coverRoot.ui.UiMode
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/** 设置页（MIUI 风格，KSU 同款卡片行样式） */
@Composable
fun SettingsMiuixScreen(
    onOpenAppearance: () -> Unit,
    onOpenServerAddress: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val preferences by repository.preferencesFlow.collectAsState(initial = AppPreferences())
    val scope = rememberCoroutineScope()
    val scrollBehavior = MiuixScrollBehavior()
    val colorScheme = MiuixTheme.colorScheme
    val enableBlur = com.coverRoot.ui.theme.LocalEnableBlur.current
    val backdrop = com.coverRoot.util.rememberBlurBackdrop(enableBlur)
    val barColor = if (backdrop != null) androidx.compose.ui.graphics.Color.Transparent
    else colorScheme.surface

    // 更新检查状态
    val updateChecker = remember { UpdateChecker(context) }
    val updateInfoState = remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    val changelogState = remember { mutableStateOf("") }
    val showUpdateDialog = remember { mutableStateOf(false) }
    val checkingUpdate = remember { mutableStateOf(false) }

    val uiModeItems = listOf(
        stringResource(R.string.mode_miuix),
        stringResource(R.string.mode_material),
    )
    val languageItems = com.coverRoot.data.LocaleHelper.supportedLanguages.map {
        if (it.code == "system") stringResource(R.string.lang_system) else it.nativeName
    }
    val languageIndex = com.coverRoot.data.LocaleHelper.supportedLanguages
        .indexOfFirst { it.code == preferences.language }
        .coerceIn(0, com.coverRoot.data.LocaleHelper.supportedLanguages.lastIndex)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            com.coverRoot.util.BlurredBar(backdrop) {
                TopAppBar(
                    title = stringResource(R.string.nav_settings),
                    color = barColor,
                    scrollBehavior = scrollBehavior,
                )
            }
        },
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 12.dp,
                    bottom = LocalScaffoldBottomPadding.current + 12.dp,
                ),
            ) {
                // 界面卡：风格下拉 + 主题入口
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        OverlayDropdownPreference(
                            title = stringResource(R.string.settings_ui_mode),
                            summary = stringResource(R.string.settings_ui_mode_summary),
                            items = uiModeItems,
                            startAction = {
                                Icon(
                                    imageVector = Icons.Rounded.DisplaySettings,
                                    contentDescription = null,
                                    tint = colorScheme.onBackground,
                                    modifier = Modifier.padding(end = 6.dp),
                                )
                            },
                            selectedIndex = if (preferences.uiMode == UiMode.Miuix.value) 0 else 1,
                            onSelectedIndexChange = { index ->
                                scope.launch {
                                    repository.setUiMode(
                                        if (index == 0) UiMode.Miuix.value else UiMode.Material.value,
                                    )
                                }
                            },
                        )
                        ArrowPreference(
                            title = stringResource(R.string.settings_theme),
                            summary = when (com.coverRoot.ui.theme.ColorMode.fromValue(preferences.colorMode)) {
                                com.coverRoot.ui.theme.ColorMode.LIGHT,
                                com.coverRoot.ui.theme.ColorMode.MONET_LIGHT -> stringResource(R.string.theme_light)
                                com.coverRoot.ui.theme.ColorMode.DARK,
                                com.coverRoot.ui.theme.ColorMode.MONET_DARK,
                                com.coverRoot.ui.theme.ColorMode.DARK_AMOLED -> stringResource(R.string.theme_dark)
                                else -> stringResource(R.string.theme_system)
                            },
                            startAction = {
                                Icon(
                                    imageVector = Icons.Rounded.Palette,
                                    contentDescription = null,
                                    tint = colorScheme.onBackground,
                                    modifier = Modifier.padding(end = 6.dp),
                                )
                            },
                            onClick = onOpenAppearance,
                        )
                    }
                }
                // 通用卡：语言
                item {
                    Card(modifier = Modifier.padding(top = 12.dp).fillMaxWidth()) {
                        OverlayDropdownPreference(
                            title = stringResource(R.string.settings_language),
                            summary = stringResource(R.string.settings_language_summary),
                            items = languageItems,
                            startAction = {
                                Icon(
                                    imageVector = Icons.Rounded.Language,
                                    contentDescription = null,
                                    tint = colorScheme.onBackground,
                                    modifier = Modifier.padding(end = 6.dp),
                                )
                            },
                            selectedIndex = languageIndex,
                            onSelectedIndexChange = { index ->
                                val lang = com.coverRoot.data.LocaleHelper.supportedLanguages.getOrNull(index)
                                if (lang != null && lang.code != preferences.language) {
                                    scope.launch { repository.setLanguage(lang.code) }
                                }
                            },
                        )
                    }
                }
                // 更新 + 后端地址卡
                item {
                    Card(modifier = Modifier.padding(top = 12.dp).fillMaxWidth()) {
                        ArrowPreference(
                            title = stringResource(R.string.settings_update),
                            summary = if (checkingUpdate.value) "检查中…"
                            else if (updateInfoState.value != null) stringResource(R.string.update_available)
                            else stringResource(R.string.settings_update_summary),
                            startAction = {
                                Icon(
                                    imageVector = Icons.Rounded.SystemUpdate,
                                    contentDescription = null,
                                    tint = colorScheme.onBackground,
                                    modifier = Modifier.padding(end = 6.dp),
                                )
                            },
                            onClick = {
                                scope.launch {
                                    checkingUpdate.value = true
                                    val info = updateChecker.checkForUpdate()
                                    updateInfoState.value = info
                                    if (info != null) {
                                        changelogState.value = updateChecker.fetchChangelog() ?: ""
                                    }
                                    checkingUpdate.value = false
                                }
                                if (updateInfoState.value != null) showUpdateDialog.value = true
                            },
                        )
                        ArrowPreference(
                            title = stringResource(R.string.nav_server_address),
                            startAction = {
                                Icon(
                                    imageVector = Icons.Rounded.VpnKey,
                                    contentDescription = null,
                                    tint = colorScheme.onBackground,
                                    modifier = Modifier.padding(end = 6.dp),
                                )
                            },
                            onClick = onOpenServerAddress,
                        )
                    }
                }
                // 隐藏图标
                item {
                    Card(modifier = Modifier.padding(top = 12.dp).fillMaxWidth()) {
                        SwitchPreference(
                            title = stringResource(R.string.settings_hide_icon),
                            summary = stringResource(R.string.settings_hide_icon_summary),
                            checked = preferences.hideAppIcon,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    repository.setHideAppIcon(checked)
                                    val api = LocalApiService(context)
                                    val action = if (checked) "tool_hide_icon" else "tool_show_icon"
                                    api.execute(action)
                                }
                            },
                        )
                    }
                }
                // 关于卡
                item {
                    Card(modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth()) {
                        ArrowPreference(
                            title = stringResource(R.string.nav_about),
                            startAction = {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = colorScheme.onBackground,
                                    modifier = Modifier.padding(end = 6.dp),
                                )
                            },
                            onClick = onOpenAbout,
                        )
                    }
                }
            }
            // 更新对话框（放在 LazyColumn 外层）
            if (showUpdateDialog.value && updateInfoState.value != null) {
                val info = updateInfoState.value!!
                UpdateDialog(
                    info = info,
                    changelog = changelogState.value,
                    onDismiss = { showUpdateDialog.value = false },
                )
            }
        }
    }
}

@Composable
private fun UpdateDialog(
    info: UpdateChecker.UpdateInfo,
    changelog: String,
    onDismiss: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.padding(20.dp),
            ) {
                androidx.compose.material3.Text(
                    text = stringResource(R.string.update_available),
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                )
                androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                androidx.compose.material3.Text(
                    text = "v${info.version} (${info.versionCode})",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                )
                if (changelog.isNotEmpty()) {
                    androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                    androidx.compose.material3.Text(
                        text = changelog,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    )
                }
                androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    androidx.compose.material3.Text(stringResource(android.R.string.ok))
                }
            }
        }
    }
}
