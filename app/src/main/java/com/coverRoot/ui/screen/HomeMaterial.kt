package com.coverRoot.ui.screen

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coverRoot.BuildConfig
import com.coverRoot.R
import com.coverRoot.data.AppConfig
import com.coverRoot.data.BackendMonitor
import com.coverRoot.data.ConfigRepository
import com.coverRoot.ui.component.material.SegmentedColumn
import com.coverRoot.ui.component.material.SegmentedListItem
import com.coverRoot.ui.navigation.LocalNavigator
import com.coverRoot.ui.navigation.Route

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMaterialScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val configRepository = remember { ConfigRepository(context) }
    val config by configRepository.configFlow.collectAsState(initial = AppConfig())
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    var online by remember { mutableStateOf(false) }
    var latencyMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(config.backendUrl) {
        val result = BackendMonitor.refresh(config.backendUrl)
        online = result.online
        latencyMs = result.latencyMs
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.nav_home)) },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Spacer(Modifier.height(4.dp))
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
            Spacer(Modifier.height(LocalScaffoldBottomPadding.current + 16.dp))
        }
    }
}

@Composable
private fun StatusCard(
    online: Boolean,
    latencyMs: Long,
    onClick: () -> Unit,
) {
    val statusIcon = if (online) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline
    val statusTitle = stringResource(if (online) R.string.home_working else R.string.home_not_working)
    val statusSummary = if (online) {
        stringResource(R.string.home_latency_ms, latencyMs)
    } else {
        stringResource(R.string.home_not_working_hint)
    }

    if (online) {
        val containerColor = MaterialTheme.colorScheme.secondaryContainer
        val contentColor = MaterialTheme.colorScheme.contentColorFor(containerColor)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = containerColor,
            contentColor = contentColor,
            shape = MaterialTheme.shapes.large,
            onClick = onClick,
        ) {
            ListItem(
                leadingContent = {
                    Icon(statusIcon, contentDescription = statusTitle)
                },
                overlineContent = null,
                supportingContent = {
                    Text(
                        text = statusSummary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                    contentColor = contentColor,
                    leadingContentColor = contentColor,
                    trailingContentColor = contentColor,
                    supportingContentColor = contentColor.copy(alpha = 0.7f),
                ),
                elevation = ListItemDefaults.elevation(),
                content = {
                    Text(
                        text = statusTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
            )
        }
    } else {
        SegmentedColumn(modifier = Modifier.fillMaxWidth()) {
            item {
                SegmentedListItem(
                    onClick = onClick,
                    leadingContent = {
                        Icon(
                            statusIcon,
                            contentDescription = statusTitle,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    headlineContent = {
                        Text(
                            statusTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    supportingContent = {
                        Text(
                            statusSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }
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
    SegmentedColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            InfoEntry(Icons.Filled.Widgets, stringResource(R.string.card_app_version), appVersion)
        }
        item {
            InfoEntry(Icons.Filled.DevicesOther, stringResource(R.string.card_device), deviceModel)
        }
        item {
            InfoEntry(Icons.Filled.Android, stringResource(R.string.card_android), androidVersion)
        }
        item {
            InfoEntry(Icons.Filled.Memory, stringResource(R.string.card_kernel), kernelVersion)
        }
    }
}

@Composable
private fun InfoEntry(
    icon: ImageVector,
    label: String,
    value: String,
) {
    SegmentedListItem(
        onClick = { },
        leadingContent = {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        headlineContent = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}
