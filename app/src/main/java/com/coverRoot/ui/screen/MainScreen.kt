package com.coverRoot.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon as MaterialIcon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar as MaterialNavigationBar
import androidx.compose.material3.NavigationBarItem as MaterialNavigationBarItem
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.coverRoot.R
import com.coverRoot.ui.LocalUiMode
import com.coverRoot.ui.UiMode
import com.coverRoot.ui.component.FloatingBottomBar
import com.coverRoot.ui.component.FloatingBottomBarItem
import com.coverRoot.ui.component.bottombar.LocalMainPagerState
import com.coverRoot.ui.component.bottombar.rememberMainPagerState
import com.coverRoot.ui.navigation.LocalNavigator
import com.coverRoot.ui.navigation.Navigator
import com.coverRoot.ui.navigation.Route
import com.coverRoot.ui.theme.LocalEnableBlur
import com.coverRoot.ui.theme.LocalEnableFloatingBottomBar
import com.coverRoot.ui.theme.LocalEnableFloatingBottomBarBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.coverRoot.util.BlurredBar
import com.coverRoot.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.utils.PagerGestureNestedScrollConnection
import top.yukonga.miuix.kmp.utils.PagerInterceptionMode
import top.yukonga.miuix.kmp.utils.pagerGestureOverride
import top.yukonga.miuix.kmp.blur.layerBackdrop

private enum class MainTab(val titleResId: Int) {
    HOME(R.string.nav_home),
    TOOLS(R.string.nav_tools),
    SETTINGS(R.string.nav_settings),
}

private val TABS = MainTab.entries
const val MAIN_TAB_COUNT = 3

@Composable
fun MainScreen() {
    val navigator = LocalNavigator.current
    val uiMode = LocalUiMode.current
    val isMaterial = uiMode == UiMode.Material
    val enableFloating = LocalEnableFloatingBottomBar.current
    val enableFloatingBlur = LocalEnableFloatingBottomBarBlur.current

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { TABS.size })
    val mainPagerState = rememberMainPagerState(pagerState = pagerState)

    val pagerBackEnabled = mainPagerState.selectedPage != 0
    val navEventState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = pagerBackEnabled,
        onBackCompleted = { mainPagerState.animateToPage(0) },
    )

    val blurBackdrop = rememberBlurBackdrop(enableBlur = LocalEnableBlur.current)
    val lensBackdrop = rememberLayerBackdrop { drawContent() }
    // M3 不使用悬浮底栏；只有 Miuix 模式 + 开启悬浮时才用
    val useFloating = !isMaterial && enableFloating
    val useLensLayer = useFloating && enableFloatingBlur

    CompositionLocalProvider(LocalMainPagerState provides mainPagerState) {
        Scaffold(
            containerColor = if (isMaterial) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                MainBottomBar(
                    isMaterial = isMaterial,
                    enableFloating = useFloating,
                    lensBackdrop = lensBackdrop,
                    blurBackdrop = blurBackdrop,
                )
            },
        ) { innerPadding ->
            val bottomPad = innerPadding.calculateBottomPadding()
            CompositionLocalProvider(
                LocalScaffoldBottomPadding provides bottomPad,
                LocalBlurBackdrop provides blurBackdrop,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (!isMaterial) Modifier.background(MiuixTheme.colorScheme.surface) else Modifier)
                        .then(if (useLensLayer) Modifier.layerBackdrop(lensBackdrop) else Modifier),
                ) {
                    HorizontalPager(
                        state = mainPagerState.pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding()
                            .pagerGestureOverride(
                                pagerState = mainPagerState.pagerState,
                                mode = PagerInterceptionMode.CrossAxisInterceptor,
                            ),
                        beyondViewportPageCount = TABS.size - 1,
                        overscrollEffect = null,
                        userScrollEnabled = false,
                        pageNestedScrollConnection = PagerGestureNestedScrollConnection,
                    ) { page ->
                        MainPage(page, navigator)
                    }
                }
            }
        }

        LaunchedEffect(mainPagerState) {
            snapshotFlow { mainPagerState.pagerState.currentPage }.collect {
                mainPagerState.syncPage()
            }
        }
    }
}

@Composable
private fun MainPage(page: Int, navigator: Navigator) {
    when (page) {
        0 -> if (LocalUiMode.current == UiMode.Material) HomeMaterialScreen() else HomeMiuixScreen()
        1 -> if (LocalUiMode.current == UiMode.Material) LogMaterialScreen() else LogMiuixScreen()
        2 -> if (LocalUiMode.current == UiMode.Material) {
            SettingsMaterialScreen(
                onOpenAppearance = { navigator.push(Route.Appearance) },
                onOpenServerAddress = { navigator.push(Route.ServerAddress) },
                onOpenAbout = { navigator.push(Route.About) },
            )
        } else {
            SettingsMiuixScreen(
                onOpenAppearance = { navigator.push(Route.Appearance) },
                onOpenServerAddress = { navigator.push(Route.ServerAddress) },
                onOpenAbout = { navigator.push(Route.About) },
            )
        }
    }
}

@Composable
private fun MainBottomBar(
    isMaterial: Boolean,
    enableFloating: Boolean,
    lensBackdrop: top.yukonga.miuix.kmp.blur.LayerBackdrop,
    blurBackdrop: top.yukonga.miuix.kmp.blur.LayerBackdrop?,
) {
    val state = LocalMainPagerState.current
    Box(modifier = Modifier.fillMaxWidth()) {
        if (enableFloating) {
            val inset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val bottom = if (inset != 0.dp) 8.dp + inset else 28.dp
            FloatingBottomBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 28.dp, end = 28.dp, bottom = bottom),
                selectedIndex = state.selectedPage,
                onSelected = { state.animateToPage(it) },
                backdrop = lensBackdrop,
                tabsCount = TABS.size,
                isBlurEnabled = LocalEnableFloatingBottomBarBlur.current,
            ) { activate ->
                TABS.forEachIndexed { index, tab ->
                    val selected = state.selectedPage == index
                    FloatingBottomBarItem(
                        selected = selected,
                        onClick = { activate(index) },
                        modifier = Modifier.defaultMinSize(minWidth = 76.dp),
                    ) {
                        MiuixIcon(
                            imageVector = if (selected) filled(tab) else outlined(tab),
                            contentDescription = stringResource(tab.titleResId),
                        )
                        MiuixText(
                            text = stringResource(tab.titleResId),
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Visible,
                        )
                    }
                }
            }
        } else if (isMaterial) {
            MaterialNavigationBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                TABS.forEachIndexed { index, tab ->
                    val selected = state.selectedPage == index
                    MaterialNavigationBarItem(
                        selected = selected,
                        onClick = { if (!selected) state.animateToPage(index) },
                        icon = {
                            MaterialIcon(
                                imageVector = if (selected) filled(tab) else outlined(tab),
                                contentDescription = stringResource(tab.titleResId),
                            )
                        },
                        label = { MaterialText(stringResource(tab.titleResId)) },
                    )
                }
            }
        } else {
            // Miuix 普通底栏：模糊开启时透明透出 BlurredBar 模糊效果；模糊关闭时用 surface 色
            val enableBlur = LocalEnableBlur.current && blurBackdrop != null
            BlurredBar(backdrop = blurBackdrop, modifier = Modifier.align(Alignment.BottomCenter)) {
                NavigationBar(color = if (enableBlur) Color.Transparent else MiuixTheme.colorScheme.surface) {
                    TABS.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = state.selectedPage == index,
                            onClick = { state.animateToPage(index) },
                            icon = if (state.selectedPage == index) filled(tab) else outlined(tab),
                            label = stringResource(tab.titleResId),
                        )
                    }
                }
            }
        }
    }
}

private fun filled(tab: MainTab) = when (tab) {
    MainTab.HOME -> Icons.Filled.Home
    MainTab.TOOLS -> Icons.Filled.Build
    MainTab.SETTINGS -> Icons.Filled.Settings
}

private fun outlined(tab: MainTab) = when (tab) {
    MainTab.HOME -> Icons.Outlined.Home
    MainTab.TOOLS -> Icons.Outlined.Build
    MainTab.SETTINGS -> Icons.Outlined.Settings
}
