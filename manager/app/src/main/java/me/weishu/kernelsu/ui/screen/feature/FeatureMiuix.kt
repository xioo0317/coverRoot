package me.weishu.kernelsu.ui.screen.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.ui.theme.LocalEnableBlur
import me.weishu.kernelsu.ui.util.BlurredBar
import me.weishu.kernelsu.ui.util.rememberBlurBackdrop
import me.weishu.kernelsu.ui.viewmodel.FeatureViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

/**
 * 功能页（Miuix 风格）：后端功能列表 + 执行结果。
 */
@Composable
fun FeaturePagerMiuix(
    uiState: FeatureViewModel.UiState,
    viewModel: FeatureViewModel,
    bottomInnerPadding: Dp,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)
    val barColor = if (enableBlur && backdrop != null) Color.Transparent else colorScheme.surface

    Scaffold(
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    color = barColor,
                    title = "功能",
                    scrollBehavior = scrollBehavior,
                    actions = {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "刷新",
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .clickable { viewModel.refresh() },
                            tint = colorScheme.onSurface,
                        )
                    },
                )
            }
        },
        popupHost = { },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .overScrollVertical()
                .padding(top = innerPadding.calculateTopPadding())
                .padding(bottom = bottomInnerPadding),
        ) {
            // 后端状态卡
            item(key = "status") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .height(10.dp)
                                .width(10.dp)
                                .background(
                                    color = when (uiState.backendOnline) {
                                        true -> Color(0xFF34C759)
                                        false -> Color(0xFFFF453A)
                                        null -> Color(0xFFFFCC00)
                                    },
                                    shape = CircleShape,
                                ),
                        )
                        Column {
                            Text(text = formatBackendSummary(uiState), fontSize = 15.sp)
                            Text(
                                text = "local_api @local_api / local_api.sock",
                                fontSize = 12.sp,
                                color = colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                }
            }
            // 功能列表
            items(uiState.features, key = { it.id }) { feature ->
                val result = uiState.results[feature.id]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { viewModel.executeFeature(feature) },
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = feature.title, fontSize = 16.sp)
                        Text(
                            text = feature.subtitle,
                            fontSize = 13.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                        )
                        if (result != null && result.prettyJson.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = result.prettyJson,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (result.ok) colorScheme.onSurface else Color(0xFFFF453A),
                            )
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(bottomInnerPadding + 32.dp)) }
        }
    }
}
