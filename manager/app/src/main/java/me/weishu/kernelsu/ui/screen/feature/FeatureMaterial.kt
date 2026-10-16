package me.weishu.kernelsu.ui.screen.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.ui.viewmodel.FeatureViewModel

/**
 * 功能页（Material 3 风格）：后端功能列表 + 执行结果。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturePagerMaterial(
    uiState: FeatureViewModel.UiState,
    viewModel: FeatureViewModel,
    bottomInnerPadding: Dp,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("功能") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "刷新")
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
        ) {
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
                                .size(10.dp)
                                .background(
                                    color = when (uiState.backendOnline) {
                                        true -> Color(0xFF34C759)
                                        false -> MaterialTheme.colorScheme.error
                                        null -> Color(0xFFFFCC00)
                                    },
                                    shape = CircleShape,
                                ),
                        )
                        Column {
                            Text(
                                text = formatBackendSummary(uiState),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = "local_api @local_api / local_api.sock",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            items(uiState.features, key = { it.id }) { feature ->
                val result = uiState.results[feature.id]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    onClick = { viewModel.executeFeature(feature) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = feature.title,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = feature.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (result != null && result.prettyJson.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = result.prettyJson,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (result.ok) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(bottomInnerPadding + 32.dp)) }
        }
    }
}
