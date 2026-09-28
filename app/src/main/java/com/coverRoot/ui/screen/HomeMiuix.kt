package com.coverRoot.ui.screen

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coverRoot.BuildConfig
import com.coverRoot.R
import com.coverRoot.data.AppConfig
import com.coverRoot.data.BackendMonitor
import com.coverRoot.data.ConfigRepository
import com.coverRoot.ui.theme.isInDarkTheme
import com.coverRoot.ui.navigation.LocalNavigator
import com.coverRoot.ui.navigation.Route
import com.coverRoot.util.BlurredBar
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun HomeMiuixScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val configRepository = remember { ConfigRepository(context) }
    val config by configRepository.configFlow.collectAsState(initial = AppConfig())
    val scrollBehavior = MiuixScrollBehavior()
    var online by remember { mutableStateOf(false) }
    var latencyMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(config.backendUrl) {
        val result = BackendMonitor.refresh(config.backendUrl)
        online = result.online
        latencyMs = result.latencyMs
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            val blurBackdrop = LocalBlurBackdrop.current
            BlurredBar(backdrop = blurBackdrop) {
                TopAppBar(title = stringResource(R.string.nav_home), scrollBehavior = scrollBehavior)
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = LocalScaffoldBottomPadding.current + 12.dp,
            ),
        ) {
            item {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatusCard(
                        online = online,
                        latencyMs = latencyMs,
                        onClick = { navigator.push(Route.ServerAddress) },
                    )
                    InfoCard(
                        appVersion = "v" + BuildConfig.VERSION_NAME,
                        deviceModel = Build.MODEL,
                        androidVersion = "Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")",
                        kernelVersion = System.getProperty("os.version") ?: "—",
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    online: Boolean,
    latencyMs: Long,
    onClick: () -> Unit,
) {
    val statusTitle = stringResource(if (online) R.string.home_working else R.string.home_not_working)
    val statusSummary = if (online) {
        stringResource(R.string.home_latency_ms, latencyMs)
    } else {
        stringResource(R.string.home_not_working_hint)
    }
    Column {
        when {
            online -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.defaultColors(
                            color = when {
                                isDynamicColor -> colorScheme.secondaryContainer
                                isInDarkTheme() -> Color(0xFF1A3825)
                                else -> Color(0xFFDFFAE4)
                            }
                        ),
                        onClick = onClick,
                        showIndication = true,
                        pressFeedbackType = PressFeedbackType.Tilt,
                    ) {
                        Box {
                            Box(
                                modifier = Modifier.fillMaxSize().offset(27.dp, 31.dp),
                                contentAlignment = Alignment.BottomEnd,
                            ) {
                                Icon(
                                    modifier = Modifier.size(110.dp),
                                    imageVector = Icons.Rounded.CheckCircleOutline,
                                    tint = if (isDynamicColor) colorScheme.primary.copy(alpha = 0.8f) else Color(0xFF36D167),
                                    contentDescription = null,
                                )
                            }
                            Box(
                                modifier = Modifier.fillMaxSize().padding(16.dp, 14.dp),
                                contentAlignment = Alignment.TopStart,
                            ) {
                                Column {
                                    Text(text = statusTitle, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(1.dp))
                                    Text(text = statusSummary, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        modifier = Modifier.weight(1f),
                        onClick = onClick,
                        showIndication = true,
                        pressFeedbackType = PressFeedbackType.Tilt,
                    ) {
                        BasicComponent(
                            title = statusTitle,
                            summary = statusSummary,
                            startAction = {
                                Icon(
                                    Icons.Rounded.ErrorOutline,
                                    statusTitle,
                                    modifier = Modifier.padding(end = 6.dp),
                                    tint = colorScheme.onBackground,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCardItem(
    icon: ImageVector,
    title: String,
    content: String,
    bottomPadding: Dp = 24.dp,
) {
    val colorScheme = MiuixTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = bottomPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.padding(end = 12.dp).size(24.dp),
            tint = colorScheme.onSurface,
        )
        Column {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colorScheme.onSurface)
            Text(
                text = content,
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun InfoCard(
    appVersion: String,
    deviceModel: String,
    androidVersion: String,
    kernelVersion: String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            InfoCardItem(Icons.Filled.Widgets, stringResource(R.string.card_app_version), appVersion)
            InfoCardItem(Icons.Filled.DevicesOther, stringResource(R.string.card_device), deviceModel)
            InfoCardItem(Icons.Filled.Android, stringResource(R.string.card_android), androidVersion)
            InfoCardItem(Icons.Filled.Memory, stringResource(R.string.card_kernel), kernelVersion, bottomPadding = 0.dp)
        }
    }
}
