package dev.abbasian.exoboost.presentation.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.abbasian.exoboost.data.local.store.HighlightConfigPrefs
import dev.abbasian.exoboost.data.local.store.HighlightPreferences
import dev.abbasian.exoboost.domain.service.HighlightManager
import dev.abbasian.exoboost.domain.service.HighlightStats
import dev.abbasian.exoboost.util.ExoBoostLogger
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun highlightSetting(
    onBack: () -> Unit,
    highlightPreferences: HighlightPreferences = koinInject(),
    highlightManager: HighlightManager = koinInject(),
    logger: ExoBoostLogger = koinInject(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val autoGenerate by highlightPreferences.autoGenerateHighlights.collectAsState(false)
    val enableCache by highlightPreferences.enableCache.collectAsState(true)
    val maxCacheSize by highlightPreferences.maxCacheSize.collectAsState(50)
    val cacheExpiryDays by highlightPreferences.cacheExpiryDays.collectAsState(30)
    val highlightConfig by highlightPreferences.highlightConfig.collectAsState(
        HighlightConfigPrefs(3000L, 30000L, 0.5f, 0.6f),
    )

    var stats by remember { mutableStateOf<HighlightStats?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        stats = highlightManager.getHighlightStats()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Highlight Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showClearDialog = true },
                    ) {
                        Icon(Icons.Filled.Delete, "Clear Cache")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            stats?.let { statistics ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Highlight Statistics",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Divider()
                        statRow("Total Videos", statistics.totalVideos.toString())
                        statRow("Total Segments", statistics.totalSegments.toString())
                        statRow(
                            "Total Duration",
                            "${statistics.totalDurationMs / 1000}s",
                        )
                        statRow(
                            "Avg Confidence",
                            "%.1f%%".format(statistics.averageConfidence * 100),
                        )
                    }
                }
            }

            Text(
                text = "General",
                style = MaterialTheme.typography.titleMedium,
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto Generate")
                            Text(
                                "Automatically generate highlights on video load",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = autoGenerate,
                            onCheckedChange = {
                                scope.launch {
                                    highlightPreferences.setAutoGenerateHighlights(it)
                                }
                            },
                        )
                    }
                }
            }

            Text(
                text = "Cache",
                style = MaterialTheme.typography.titleMedium,
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable Cache")
                            Text(
                                "Save generated highlights for faster access",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = enableCache,
                            onCheckedChange = {
                                scope.launch {
                                    highlightPreferences.setEnableCache(it)
                                }
                            },
                        )
                    }

                    if (enableCache) {
                        Divider()

                        Column {
                            Text("Max Cache Size: $maxCacheSize videos")
                            Slider(
                                value = maxCacheSize.toFloat(),
                                onValueChange = {
                                    scope.launch {
                                        highlightPreferences.setMaxCacheSize(it.toInt())
                                    }
                                },
                                valueRange = 10f..200f,
                                steps = 18,
                            )
                        }

                        Column {
                            Text("Cache Expiry: $cacheExpiryDays days")
                            Slider(
                                value = cacheExpiryDays.toFloat(),
                                onValueChange = {
                                    scope.launch {
                                        highlightPreferences.setCacheExpiryDays(it.toInt())
                                    }
                                },
                                valueRange = 1f..90f,
                                steps = 88,
                            )
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    highlightManager.cleanExpiredCache()
                                    stats = highlightManager.getHighlightStats()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Clean Expired Cache")
                        }
                    }
                }
            }

            Text(
                text = "Analysis",
                style = MaterialTheme.typography.titleMedium,
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column {
                        Text(
                            "Min Duration: ${highlightConfig.minDuration / 1000}s",
                        )
                        Slider(
                            value = highlightConfig.minDuration.toFloat(),
                            onValueChange = { value ->
                                scope.launch {
                                    highlightPreferences.updateHighlightConfig(
                                        highlightConfig.copy(minDuration = value.toLong()),
                                    )
                                }
                            },
                            valueRange = 1000f..10000f,
                            steps = 8,
                        )
                    }

                    Column {
                        Text(
                            "Max Duration: ${highlightConfig.maxDuration / 1000}s",
                        )
                        Slider(
                            value = highlightConfig.maxDuration.toFloat(),
                            onValueChange = { value ->
                                scope.launch {
                                    highlightPreferences.updateHighlightConfig(
                                        highlightConfig.copy(maxDuration = value.toLong()),
                                    )
                                }
                            },
                            valueRange = 10000f..60000f,
                            steps = 9,
                        )
                    }

                    Column {
                        Text(
                            "Audio Threshold: %.1f".format(highlightConfig.audioThreshold),
                        )
                        Slider(
                            value = highlightConfig.audioThreshold,
                            onValueChange = { value ->
                                scope.launch {
                                    highlightPreferences.updateHighlightConfig(
                                        highlightConfig.copy(audioThreshold = value),
                                    )
                                }
                            },
                            valueRange = 0f..1f,
                        )
                    }

                    Column {
                        Text(
                            "Motion Threshold: %.1f".format(highlightConfig.motionThreshold),
                        )
                        Slider(
                            value = highlightConfig.motionThreshold,
                            onValueChange = { value ->
                                scope.launch {
                                    highlightPreferences.updateHighlightConfig(
                                        highlightConfig.copy(motionThreshold = value),
                                    )
                                }
                            },
                            valueRange = 0f..1f,
                        )
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Cache?") },
            text = {
                Text("This will delete all cached highlights. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            highlightManager.clearAllHighlights()
                            stats = highlightManager.getHighlightStats()
                            showClearDialog = false
                        }
                    },
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun statRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
