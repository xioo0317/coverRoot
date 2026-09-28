package com.coverRoot.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coverRoot.R
import com.coverRoot.data.LogRepository
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogMaterialScreen() {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var logLines by remember { mutableStateOf<List<String>>(emptyList()) }
    var isEmpty by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        val lines = LogRepository.readLogLines(context)
        logLines = lines
        isEmpty = lines.isEmpty()
    }
    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.nav_tools)) },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { paddingValues ->
        if (isEmpty) {
            LogPlaceholder(
                modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).nestedScroll(scrollBehavior.nestedScrollConnection),
                miuix = false,
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.large,
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
                        contentPadding = PaddingValues(12.dp),
                    ) {
                        items(logLines) { line ->
                            Text(
                                text = line.ifEmpty { " " },
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 1.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogMiuixScreen() {
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior()
    var logLines by remember { mutableStateOf<List<String>>(emptyList()) }
    var isEmpty by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        val lines = LogRepository.readLogLines(context)
        logLines = lines
        isEmpty = lines.isEmpty()
    }
    MiuixScaffold(
        topBar = { TopAppBar(title = stringResource(R.string.nav_tools), scrollBehavior = scrollBehavior) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        if (isEmpty) {
            LazyColumn(
                modifier = Modifier.fillMaxHeight().scrollEndHaptic().overScrollVertical().nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = LocalScaffoldBottomPadding.current + 12.dp,
                ),
            ) {
                item {
                    LogPlaceholder(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        miuix = true,
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize().padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = LocalScaffoldBottomPadding.current + 12.dp,
                    start = 12.dp,
                    end = 12.dp,
                ),
            ) {
                top.yukonga.miuix.kmp.basic.Card(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().scrollEndHaptic().overScrollVertical().nestedScroll(scrollBehavior.nestedScrollConnection),
                        contentPadding = PaddingValues(12.dp),
                    ) {
                        items(logLines) { line ->
                            MiuixText(
                                text = line.ifEmpty { " " },
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier.padding(vertical = 1.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogPlaceholder(modifier: Modifier = Modifier, miuix: Boolean) {
    val title = stringResource(R.string.log_empty)
    val summary = stringResource(R.string.log_empty_hint)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorically,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            if (miuix) {
                MiuixIcon(Icons.Rounded.Description, contentDescription = null,
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary, modifier = Modifier.size(56.dp))
                MiuixText(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MiuixTheme.colorScheme.onSurface)
                MiuixText(summary, fontSize = 14.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            } else {
                Icon(Icons.Rounded.Description, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(56.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
