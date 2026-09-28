package com.coverRoot.ui.screen

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.LayerBackdrop

val LocalScaffoldBottomPadding = staticCompositionLocalOf { 0.dp }
val LocalBlurBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }
