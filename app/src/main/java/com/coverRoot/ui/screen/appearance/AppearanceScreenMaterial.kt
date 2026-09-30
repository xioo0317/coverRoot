package com.coverRoot.ui.screen.appearance

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BlurLinear
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.CallToAction
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DesignServices
import androidx.compose.material.icons.rounded.MenuOpen
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.coverRoot.R
import com.coverRoot.data.AppPreferences
import com.coverRoot.data.SettingsRepository
import com.coverRoot.ui.component.material.ExpressiveScaffold
import com.coverRoot.ui.component.material.SegmentedColumn
import com.coverRoot.ui.component.material.SegmentedDropdownItem
import com.coverRoot.ui.component.material.SegmentedSwitchItem
import com.coverRoot.ui.component.material.TonalCard
import com.coverRoot.ui.component.material.TopBarBackButton
import com.coverRoot.ui.component.material.expressiveTopAppBarColors
import com.coverRoot.ui.navigation.LocalNavigator
import com.coverRoot.ui.theme.ColorMode
import com.coverRoot.ui.theme.keyColorOptions
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import kotlinx.coroutines.launch

@Composable
fun AppearanceMaterial() {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val prefs by repository.preferencesFlow.collectAsState(initial = AppPreferences())
    val scope = rememberCoroutineScope()
    val navigator = LocalNavigator.current
    val haptic = LocalHapticFeedback.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val colorMode = ColorMode.fromValue(prefs.colorMode)
    val isDark = if (colorMode.isSystem) isSystemInDarkTheme() else colorMode.isDark
    val isAmoled = colorMode.isAmoled
    val paletteStyle = try { PaletteStyle.valueOf(prefs.colorStyle) } catch (_: Exception) { PaletteStyle.TonalSpot }
    val colorSpec = try { ColorSpec.SpecVersion.valueOf(prefs.colorSpec) } catch (_: Exception) { ColorSpec.SpecVersion.SPEC_2025 }

    ExpressiveScaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                navigationIcon = { TopBarBackButton(onClick = { navigator.pop() }) },
                title = { Text(stringResource(R.string.settings_theme)) },
                colors = expressiveTopAppBarColors(),
                windowInsets = WindowInsets(0),
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { paddingValues ->
        val navBars = WindowInsets.navigationBars.asPaddingValues()
        val captionBarInset = WindowInsets.captionBar.asPaddingValues()
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            item {
                ThemePreviewCard(prefs.keyColor, isDark, isAmoled, paletteStyle, colorSpec)
            }
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        ColorSwatch(Color.Unspecified, prefs.keyColor == 0, isDark, isAmoled, paletteStyle, colorSpec) {
                            scope.launch { repository.setKeyColor(0) }
                        }
                    }
                    items(keyColorOptions) { c ->
                        ColorSwatch(Color(c), prefs.keyColor == c, isDark, isAmoled, paletteStyle, colorSpec) {
                            scope.launch { repository.setKeyColor(c) }
                        }
                    }
                }
            }
            item {
                val options = listOf(
                    ColorMode.SYSTEM to Icons.Filled.Brightness4,
                    ColorMode.LIGHT to Icons.Filled.Brightness7,
                    ColorMode.DARK to Icons.Filled.Brightness3,
                    ColorMode.DARK_AMOLED to Icons.Filled.Brightness1,
                )
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                    ) {
                        options.forEachIndexed { index, (mode, icon) ->
                            ToggleButton(
                                checked = colorMode == mode,
                                onCheckedChange = {
                                    if (it) {
                                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                        scope.launch { repository.setColorMode(mode.value) }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ToggleButtonDefaults.colors(
                                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                                    checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                ),
                                shapes = when (index) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                },
                            ) { Icon(icon, null) }
                        }
                    }
                }
            }
            item {
                val styles = PaletteStyle.entries
                val specs = ColorSpec.SpecVersion.entries
                SegmentedColumn(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    content = listOf(
                        {
                            SegmentedDropdownItem(
                                icon = Icons.Rounded.Style,
                                title = stringResource(R.string.settings_color_style),
                                items = styles.map { it.name },
                                selectedIndex = styles.indexOf(paletteStyle),
                                onItemSelected = { i -> scope.launch { repository.setColorStyle(styles[i].name) } },
                            )
                        },
                        {
                            SegmentedDropdownItem(
                                icon = Icons.Rounded.DesignServices,
                                title = stringResource(R.string.settings_color_spec),
                                items = specs.map { it.name },
                                selectedIndex = specs.indexOf(colorSpec).coerceAtLeast(0),
                                onItemSelected = { i -> scope.launch { repository.setColorSpec(specs[i].name) } },
                            )
                        },
                    ),
                )
            }
            item {
                SegmentedColumn(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    content = listOf(
                        {
                            SegmentedSwitchItem(
                                icon = Icons.Rounded.BlurOn,
                                title = stringResource(R.string.settings_enable_blur),
                                summary = stringResource(R.string.settings_enable_blur_summary),
                                checked = prefs.enableBlur,
                                onCheckedChange = { scope.launch { repository.setEnableBlur(it) } },
                            )
                        },
                    ),
                )
            }
            item {
                AnimatedVisibility(visible = prefs.enableBlur) {
                    TonalCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            val intensityState = rememberSliderState(prefs.blurIntensity, trackRange = 10f..60f)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.BlurLinear, stringResource(R.string.settings_blur_intensity),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(12.dp))
                                Text("${intensityState.value.toInt()}", modifier = Modifier.weight(1f))
                            }
                            Slider(state = intensityState, onValueChangeFinished = {
                                scope.launch { repository.setBlurIntensity(intensityState.value) }
                            })
                            val alphaState = rememberSliderState(prefs.blurAlpha, trackRange = 0.3f..1.0f)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Opacity, stringResource(R.string.settings_blur_alpha),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(12.dp))
                                Text("${(alphaState.value * 100).toInt()}%", modifier = Modifier.weight(1f))
                            }
                            Slider(state = alphaState, onValueChangeFinished = {
                                scope.launch { repository.setBlurAlpha(alphaState.value) }
                            })
                        }
                    }
                }
            }
            // 导航外观：悬浮底栏 / 液态玻璃 / 导航徽章
            item {
                SegmentedColumn(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    content = listOf(
                        {
                            SegmentedSwitchItem(
                                icon = Icons.Rounded.CallToAction,
                                title = stringResource(R.string.settings_floating_bottom_bar),
                                summary = stringResource(R.string.settings_floating_bottom_bar_summary),
                                checked = prefs.enableFloatingBottomBar,
                                onCheckedChange = { scope.launch { repository.setEnableFloatingBottomBar(it) } },
                            )
                        },
                        {
                            AnimatedVisibility(visible = prefs.enableFloatingBottomBar && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                SegmentedSwitchItem(
                                    icon = Icons.Rounded.WaterDrop,
                                    title = stringResource(R.string.settings_enable_glass),
                                    summary = stringResource(R.string.settings_enable_glass_summary),
                                    checked = prefs.enableFloatingBottomBarBlur,
                                    onCheckedChange = { scope.launch { repository.setEnableFloatingBottomBarBlur(it) } },
                                )
                            }
                        },
                        {
                            SegmentedSwitchItem(
                                icon = Icons.Rounded.Pin,
                                title = stringResource(R.string.settings_navigation_badge),
                                summary = stringResource(R.string.settings_navigation_badge_summary),
                                checked = prefs.enableNavigationBadge,
                                onCheckedChange = { scope.launch { repository.setEnableNavigationBadge(it) } },
                            )
                        },
                    ),
                )
            }
            // 手势：预测返回 / 滑动返回 / 分页手势模式
            item {
                SegmentedColumn(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    content = buildList {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            add {
                                SegmentedSwitchItem(
                                    icon = Icons.Rounded.MenuOpen,
                                    title = stringResource(R.string.settings_enable_predictive_back),
                                    summary = stringResource(R.string.settings_enable_predictive_back_summary),
                                    checked = prefs.enablePredictiveBack,
                                    onCheckedChange = { scope.launch { repository.setEnablePredictiveBack(it) } },
                                )
                            }
                        }
                        add {
                            SegmentedSwitchItem(
                                icon = Icons.Rounded.Swipe,
                                title = stringResource(R.string.settings_enable_swipe_dismiss),
                                summary = stringResource(R.string.settings_enable_swipe_dismiss_summary),
                                checked = prefs.enableSwipeDismiss,
                                onCheckedChange = { scope.launch { repository.setEnableSwipeDismiss(it) } },
                            )
                        }
                        add {
                            SegmentedDropdownItem(
                                icon = Icons.Rounded.ViewCarousel,
                                title = stringResource(R.string.settings_pager_gesture_mode),
                                items = listOf(
                                    stringResource(R.string.settings_pager_gesture_native),
                                    stringResource(R.string.settings_pager_gesture_cross_axis),
                                    stringResource(R.string.settings_pager_gesture_ios_like),
                                ),
                                selectedIndex = prefs.pagerInterceptionMode.coerceIn(0, 2),
                                onItemSelected = { i -> scope.launch { repository.setPagerInterceptionMode(i) } },
                            )
                        }
                    },
                )
            }
            item {
                TonalCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    val sliderState = rememberSliderState(prefs.pageScale, trackRange = 0.8f..1.1f)
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AspectRatio, stringResource(R.string.settings_page_scale),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_page_scale), style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(R.string.settings_page_scale_summary), style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${(sliderState.value * 100).toInt()}%")
                        }
                        Slider(state = sliderState, modifier = Modifier.fillMaxWidth(), onValueChangeFinished = {
                            scope.launch { repository.setPageScale(sliderState.value) }
                        })
                    }
                }
            }
            item {
                Spacer(Modifier.height(16.dp + navBars.calculateBottomPadding() + captionBarInset.calculateBottomPadding()))
            }
        }
    }
}

@Composable
private fun ThemePreviewCard(
    keyColor: Int, isDark: Boolean, isAmoled: Boolean,
    paletteStyle: PaletteStyle, colorSpec: ColorSpec.SpecVersion,
) {
    val configuration = LocalConfiguration.current
    val ratio = configuration.screenWidthDp.toFloat() / configuration.screenHeightDp.toFloat()
    val scheme = previewScheme(keyColor, isDark, isAmoled, paletteStyle, colorSpec)
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.4f).aspectRatio(ratio),
            color = scheme.surfaceContainer,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, scheme.outlineVariant),
        ) {
            Column {
                Box(modifier = Modifier.height(48.dp).fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxSize().padding(start = 12.dp, top = 16.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
                    }
                }
                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    val showInfo = maxHeight >= 72.dp
                    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        TonalCard(containerColor = scheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = RoundedCornerShape(8.dp), content = {})
                        if (showInfo) {
                            TonalCard(containerColor = scheme.surfaceBright,
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                shape = RoundedCornerShape(8.dp), content = {})
                        }
                    }
                }
                Surface(color = scheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.height(40.dp).fillMaxWidth().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Home, null, tint = scheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color, selected: Boolean, isDark: Boolean, isAmoled: Boolean,
    paletteStyle: PaletteStyle, colorSpec: ColorSpec.SpecVersion, onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val keyColor = if (color == Color.Unspecified) 0 else color.toArgb()
    val scheme = previewScheme(keyColor, isDark, isAmoled, paletteStyle, colorSpec)
    Surface(
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.VirtualKey); onClick() },
        shape = RoundedCornerShape(20.dp), color = scheme.surfaceContainer, modifier = Modifier.size(72.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(48.dp)) {
                drawArc(scheme.primaryContainer, 180f, 180f, true)
                drawArc(scheme.tertiaryContainer, 0f, 180f, true)
            }
            val scale by animateFloatAsState(if (selected) 1.1f else 1.0f)
            Box(modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }) {
                AnimatedVisibility(visible = selected,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f)) {
                    Box(modifier = Modifier.size(56.dp).border(2.dp, scheme.primary), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(scheme.primary),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Check, null, tint = scheme.onPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                AnimatedVisibility(visible = !selected,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f)) {
                    Box(modifier = Modifier.size(20.dp).background(scheme.primary))
                }
            }
        }
    }
}

@Composable
private fun previewScheme(
    keyColor: Int, isDark: Boolean, isAmoled: Boolean,
    paletteStyle: PaletteStyle, colorSpec: ColorSpec.SpecVersion,
): androidx.compose.material3.ColorScheme {
    val context = LocalContext.current
    val base = when {
        keyColor != 0 -> rememberDynamicColorScheme(Color(keyColor), isDark, style = paletteStyle, specVersion = colorSpec)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (isDark) androidx.compose.material3.dynamicDarkColorScheme(context) else androidx.compose.material3.dynamicLightColorScheme(context)
        else -> if (isDark) androidx.compose.material3.darkColorScheme(primary = Color(0xFFB3C5FF))
            else androidx.compose.material3.lightColorScheme(primary = Color(0xFF3A6AE0))
    }
    return if (isDark && isAmoled) base.copy(
        background = Color.Black, surface = Color.Black,
        surfaceContainer = Color(0xFF0A0A0A), surfaceBright = Color(0xFF1B1B1B), surfaceDim = Color.Black,
    ) else base
}
