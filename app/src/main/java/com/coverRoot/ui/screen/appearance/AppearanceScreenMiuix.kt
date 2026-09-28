package com.coverRoot.ui.screen.appearance

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.DesignServices
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coverRoot.R
import com.coverRoot.data.AppPreferences
import com.coverRoot.data.SettingsRepository
import com.coverRoot.ui.component.miuix.ScaleDialog
import com.coverRoot.ui.navigation.LocalNavigator
import com.coverRoot.ui.theme.ColorMode
import com.coverRoot.ui.theme.LocalEnableBlur
import com.coverRoot.ui.theme.colorNameResIds
import com.coverRoot.ui.theme.keyColorOptions
import com.coverRoot.util.BlurredBar
import com.coverRoot.util.rememberBlurBackdrop
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun AppearanceMiuix() {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val prefs by repository.preferencesFlow.collectAsState(initial = AppPreferences())
    val scope = rememberCoroutineScope()
    val navigator = LocalNavigator.current
    val scrollBehavior = MiuixScrollBehavior()
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)
    val barColor = if (backdrop != null) Color.Transparent else colorScheme.surface
    val colorMode = ColorMode.fromValue(prefs.colorMode)
    val isDark = colorMode.isDark || (colorMode.isSystem && isSystemInDarkTheme())
    val showScaleDialog = rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    color = barColor,
                    title = stringResource(R.string.settings_theme),
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            val dir = LocalLayoutDirection.current
                            Icon(
                                modifier = Modifier.graphicsLayer {
                                    if (dir == LayoutDirection.Rtl) scaleX = -1f
                                },
                                imageVector = MiuixIcons.Back,
                                contentDescription = null,
                                tint = colorScheme.onBackground,
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars
            .add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .padding(horizontal = 12.dp),
                contentPadding = innerPadding,
                overscrollEffect = null,
            ) {
                item {
                    Spacer(Modifier.height(32.dp))
                    ThemePreviewCardMiuix(
                        keyColor = prefs.keyColor,
                        isDark = isDark,
                        miuixMonet = prefs.miuixMonet,
                        paletteStyle = prefs.colorStyle,
                        colorSpec = prefs.colorSpec,
                    )
                    Spacer(Modifier.height(72.dp))
                    val modes = listOf(
                        stringResource(R.string.theme_system),
                        stringResource(R.string.theme_light),
                        stringResource(R.string.theme_dark),
                    )
                    val baseIndex = if (colorMode.value >= 3) colorMode.value - 3 else colorMode.value
                    TabRow(
                        tabs = modes,
                        selectedTabIndex = baseIndex.coerceIn(0, 2),
                        onTabSelected = { idx ->
                            val value = if (prefs.miuixMonet) idx + 3 else idx
                            scope.launch { repository.setColorMode(value) }
                        },
                    )
                    Card(modifier = Modifier.padding(top = 12.dp).fillMaxWidth()) {
                        SwitchPreference(
                            title = stringResource(R.string.settings_monet),
                            startAction = {
                                Icon(Icons.Rounded.Wallpaper, Modifier.padding(end = 6.dp),
                                    stringResource(R.string.settings_monet), tint = colorScheme.onBackground)
                            },
                            checked = prefs.miuixMonet,
                            onCheckedChange = { monet ->
                                scope.launch {
                                    repository.setMiuixMonet(monet)
                                    val current = ColorMode.fromValue(prefs.colorMode)
                                    val next = if (monet) current.toMonetMode() else current.toNonMonetMode()
                                    repository.setColorMode(next)
                                }
                            },
                        )
                        AnimatedVisibility(visible = prefs.miuixMonet) {
                            Column {
                                val values = listOf(0) + keyColorOptions
                                val items = listOf(stringResource(R.string.settings_key_color_default)) +
                                    colorNameResIds.map { stringResource(it) }
                                OverlayDropdownPreference(
                                    title = stringResource(R.string.settings_key_color),
                                    items = items,
                                    startAction = {
                                        Icon(Icons.Rounded.Colorize, Modifier.padding(end = 6.dp),
                                            stringResource(R.string.settings_key_color), tint = colorScheme.onBackground)
                                    },
                                    selectedIndex = values.indexOf(prefs.keyColor).coerceAtLeast(0),
                                    onSelectedIndexChange = { i ->
                                        scope.launch { repository.setKeyColor(values[i]) }
                                    },
                                )
                                AnimatedVisibility(visible = prefs.keyColor != 0) {
                                    Column {
                                        val styles = PaletteStyle.entries
                                        OverlayDropdownPreference(
                                            title = stringResource(R.string.settings_color_style),
                                            startAction = {
                                                Icon(Icons.Rounded.Style, Modifier.padding(end = 6.dp),
                                                    stringResource(R.string.settings_color_style), tint = colorScheme.onBackground)
                                            },
                                            items = styles.map { it.name },
                                            selectedIndex = styles.indexOfFirst { it.name == prefs.colorStyle }.coerceAtLeast(0),
                                            onSelectedIndexChange = { i ->
                                                scope.launch { repository.setColorStyle(styles[i].name) }
                                            },
                                        )
                                        val specs = ColorSpec.SpecVersion.entries
                                        OverlayDropdownPreference(
                                            title = stringResource(R.string.settings_color_spec),
                                            startAction = {
                                                Icon(Icons.Rounded.DesignServices, Modifier.padding(end = 6.dp),
                                                    stringResource(R.string.settings_color_spec), tint = colorScheme.onBackground)
                                            },
                                            items = specs.map { it.name },
                                            selectedIndex = specs.indexOfFirst { it.name == prefs.colorSpec }.coerceAtLeast(0),
                                            onSelectedIndexChange = { i ->
                                                scope.launch { repository.setColorSpec(specs[i].name) }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Card(modifier = Modifier.padding(top = 12.dp).fillMaxWidth()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            SwitchPreference(
                                title = stringResource(R.string.settings_enable_blur),
                                summary = stringResource(R.string.settings_enable_blur_summary),
                                startAction = {
                                    Icon(Icons.Rounded.BlurOn, Modifier.padding(end = 6.dp),
                                        stringResource(R.string.settings_enable_blur), tint = colorScheme.onBackground)
                                },
                                checked = prefs.enableBlur,
                                onCheckedChange = { scope.launch { repository.setEnableBlur(it) } },
                            )
                        }
                        AnimatedVisibility(visible = prefs.enableBlur) {
                            Column {
                                var blurIntensity by remember(prefs.blurIntensity) { mutableFloatStateOf(prefs.blurIntensity) }
                                ArrowPreference(
                                    title = stringResource(R.string.settings_blur_intensity),
                                    summary = "${blurIntensity.toInt()}",
                                    startAction = {
                                        Icon(Icons.Rounded.BlurOn, Modifier.padding(end = 6.dp),
                                            stringResource(R.string.settings_blur_intensity), tint = colorScheme.onBackground)
                                    },
                                    endActions = {
                                        Text("${blurIntensity.toInt()}", color = colorScheme.onSurfaceVariantActions)
                                    },
                                    onClick = {},
                                    holdDownState = false,
                                    bottomAction = {
                                        Slider(
                                            value = blurIntensity,
                                            onValueChange = { blurIntensity = it },
                                            onValueChangeFinished = { scope.launch { repository.setBlurIntensity(blurIntensity) } },
                                            valueRange = 10f..60f,
                                        )
                                    },
                                    modifier = Modifier.padding(top = 12.dp),
                                )
                                var blurAlpha by remember(prefs.blurAlpha) { mutableFloatStateOf(prefs.blurAlpha) }
                                ArrowPreference(
                                    title = stringResource(R.string.settings_blur_alpha),
                                    summary = "${(blurAlpha * 100).toInt()}%",
                                    startAction = {
                                        Icon(Icons.Rounded.Colorize, Modifier.padding(end = 6.dp),
                                            stringResource(R.string.settings_blur_alpha), tint = colorScheme.onBackground)
                                    },
                                    endActions = {
                                        Text("${(blurAlpha * 100).toInt()}%", color = colorScheme.onSurfaceVariantActions)
                                    },
                                    onClick = {},
                                    holdDownState = false,
                                    bottomAction = {
                                        Slider(
                                            value = blurAlpha,
                                            onValueChange = { blurAlpha = it },
                                            onValueChangeFinished = { scope.launch { repository.setBlurAlpha(blurAlpha) } },
                                            valueRange = 0.3f..1.0f,
                                        )
                                    },
                                    modifier = Modifier.padding(top = 12.dp),
                                )
                            }
                        }
                    }
                    Card(modifier = Modifier.padding(top = 12.dp).fillMaxWidth()) {
                        var sliderValue by remember(prefs.pageScale) { mutableFloatStateOf(prefs.pageScale) }
                        ArrowPreference(
                            title = stringResource(R.string.settings_page_scale),
                            summary = stringResource(R.string.settings_page_scale_summary),
                            startAction = {
                                Icon(Icons.Rounded.AspectRatio, Modifier.padding(end = 6.dp),
                                    stringResource(R.string.settings_page_scale), tint = colorScheme.onBackground)
                            },
                            endActions = {
                                Text("${(sliderValue * 100).toInt()}%", color = colorScheme.onSurfaceVariantActions)
                            },
                            onClick = { showScaleDialog.value = !showScaleDialog.value },
                            holdDownState = showScaleDialog.value,
                            bottomAction = {
                                Slider(
                                    value = sliderValue,
                                    onValueChange = { sliderValue = it },
                                    onValueChangeFinished = { scope.launch { repository.setPageScale(sliderValue) } },
                                    valueRange = 0.8f..1.1f,
                                    showKeyPoints = true,
                                    keyPoints = listOf(0.8f, 0.9f, 1f, 1.1f),
                                    magnetThreshold = 0.01f,
                                    hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                                )
                            },
                        )
                        ScaleDialog(
                            show = showScaleDialog.value,
                            onDismissRequest = { showScaleDialog.value = false },
                            scaleState = { prefs.pageScale },
                            onScaleChange = { scope.launch { repository.setPageScale(it) } },
                        )
                    }
                }
                item {
                    Spacer(
                        Modifier.height(
                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                                WindowInsets.captionBar.asPaddingValues().calculateBottomPadding() + 12.dp,
                        ),
                    )
                }
            }
        }
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
private fun ThemePreviewCardMiuix(
    keyColor: Int,
    isDark: Boolean,
    miuixMonet: Boolean,
    paletteStyle: String = "TonalSpot",
    colorSpec: String = "SPEC_2021",
) {
    val configuration = LocalConfiguration.current
    val ratio = configuration.screenWidthDp.toFloat() / configuration.screenHeightDp.toFloat()
    val style = try { PaletteStyle.valueOf(paletteStyle) } catch (_: Exception) { PaletteStyle.TonalSpot }
    val spec = if (colorSpec == "SPEC_2025") ColorSpec.SpecVersion.SPEC_2025 else ColorSpec.SpecVersion.SPEC_2021
    val seedColor = if (keyColor == 0) colorScheme.primary else Color(keyColor)
    val effectiveStyle = if (keyColor == 0) PaletteStyle.TonalSpot else style
    val effectiveSpec = if (keyColor == 0) ColorSpec.SpecVersion.Default else spec
    val dynamicCs = rememberDynamicColorScheme(seedColor, isDark, effectiveStyle, effectiveSpec)
    val bgColor = if (miuixMonet) dynamicCs.background else colorScheme.surface
    val textColor = if (miuixMonet) dynamicCs.onSurface else colorScheme.onBackground
    val accentCardColor = when {
        miuixMonet -> dynamicCs.secondaryContainer
        isDark -> Color(0xFF1A3825)
        else -> Color(0xFFDFFAE4)
    }
    val cardColor = if (miuixMonet) dynamicCs.surfaceContainerHighest else colorScheme.surfaceVariant
    val navBarColor = if (miuixMonet) dynamicCs.surfaceContainer else colorScheme.surface
    val iconColor = if (miuixMonet) dynamicCs.primary else colorScheme.primary
    val navSelected = colorScheme.onSurfaceContainer
    val navUnselected = colorScheme.onSurfaceContainer.copy(alpha = 0.5f)

    Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.TopCenter) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .aspectRatio(ratio)
                .clip(RoundedCornerShape(20.dp))
                .background(bgColor)
                .border(1.dp, colorScheme.outline, RoundedCornerShape(20.dp)),
        ) {
            Column {
                Row(
                    modifier = Modifier.height(48.dp).fillMaxWidth().padding(start = 12.dp, top = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.app_name), fontSize = 12.sp, color = textColor)
                }
                Box(
                    modifier = Modifier.fillMaxWidth().height(45.dp).padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(6.dp)).background(accentCardColor),
                )
                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    val count = when {
                        maxHeight >= 96.dp -> 2
                        maxHeight >= 72.dp -> 1
                        else -> 0
                    }
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(6.dp)).background(cardColor))
                        repeat(count) {
                            Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(cardColor))
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            ) {
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(textColor.copy(alpha = 0.1f)))
                Row(
                    modifier = Modifier.height(36.dp).fillMaxWidth().background(navBarColor).padding(top = 2.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(3) {
                        Box(Modifier.size(15.dp).clip(RoundedCornerShape(3.dp)).background(if (it == 0) navSelected else navUnselected))
                    }
                }
            }
        }
    }
}
