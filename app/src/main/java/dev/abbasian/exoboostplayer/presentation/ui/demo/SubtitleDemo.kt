package dev.abbasian.exoboostplayer.presentation.ui.demo

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SubtitlesOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abbasian.exoboost.domain.model.MediaPlayerConfig
import dev.abbasian.exoboost.domain.model.SubtitleStyle
import dev.abbasian.exoboost.domain.model.SubtitleTrack
import dev.abbasian.exoboost.presentation.ui.component.subtitleBottomSheet
import dev.abbasian.exoboost.presentation.ui.screen.exoBoostPlayer
import dev.abbasian.exoboost.presentation.viewmodel.MediaPlayerViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun SubtitleDemo() {
    val viewModel: MediaPlayerViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val availableSubtitles by viewModel.availableSubtitles.collectAsStateWithLifecycle()
    val currentSubtitle by viewModel.currentSubtitle.collectAsStateWithLifecycle()
    val subtitleStyle by viewModel.subtitleStyle.collectAsStateWithLifecycle()
    val showSubtitleSheet by viewModel.showSubtitleSheet.collectAsStateWithLifecycle()

    var showInfoPanel by remember { mutableStateOf(true) }
    var isSearching by remember { mutableStateOf(false) }
    var searchMessage by remember { mutableStateOf("") }

    val videoUrl =
        "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"

    val subtitlePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            uri?.let {
                viewModel.loadExternalSubtitle(it, "Custom Subtitle")
            }
        }

    LaunchedEffect(availableSubtitles.size, isSearching) {
        if (isSearching && availableSubtitles.isNotEmpty()) {
            kotlinx.coroutines.delay(500)
            isSearching = false
            searchMessage = "Found ${availableSubtitles.size} subtitle(s) online!"
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        exoBoostPlayer(
            videoUrl = videoUrl,
            mediaConfig =
                MediaPlayerConfig(
                    autoPlay = true,
                    enableSmartHighlights = false,
                    enableSubtitles = true,
                ),
            modifier = Modifier.fillMaxSize(),
            viewModel = viewModel,
        )

        if (showInfoPanel) {
            SubtitleInfoPanel(
                availableSubtitles = availableSubtitles,
                currentSubtitle = currentSubtitle,
                subtitleStyle = subtitleStyle,
                isSearching = isSearching,
                searchMessage = searchMessage,
                onDismiss = { showInfoPanel = false },
                onOpenSettings = { viewModel.toggleSubtitleSheet(true) },
                onSearchSubtitles = {
                    isSearching = true
                    searchMessage = "Searching..."
                    viewModel.searchSubtitles(videoUrl, "Big Buck Bunny")
                },
                onBrowseFile = {
                    subtitlePickerLauncher.launch("*/*")
                },
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
            )
        }

        if (!showInfoPanel) {
            FloatingActionButton(
                onClick = { showInfoPanel = true },
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                containerColor = Color.Black.copy(alpha = 0.7f),
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Show info",
                    tint = Color.White,
                )
            }
        }

        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isSearching) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        searchMessage,
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            } else if (searchMessage.isNotEmpty()) {
                Text(
                    searchMessage,
                    color = if (availableSubtitles.isEmpty()) Color.Yellow else Color.Green,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ElevatedButton(
                    onClick = {
                        isSearching = true
                        searchMessage = "Searching online..."
                        viewModel.searchSubtitles(videoUrl, "Big Buck Bunny")
                    },
                    colors =
                        ButtonDefaults.elevatedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            contentColor = Color.White,
                        ),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Search Online", style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.width(8.dp))

                ElevatedButton(
                    onClick = { subtitlePickerLauncher.launch("*/*") },
                    colors =
                        ButtonDefaults.elevatedButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                            contentColor = Color.White,
                        ),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Browse File", style = MaterialTheme.typography.bodySmall)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ElevatedButton(
                    onClick = { viewModel.toggleSubtitleSheet(true) },
                    colors =
                        ButtonDefaults.elevatedButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.7f),
                            contentColor = Color.White,
                        ),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Settings", style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.width(8.dp))

                ElevatedButton(
                    onClick = { viewModel.selectSubtitle(null) },
                    colors =
                        ButtonDefaults.elevatedButtonColors(
                            containerColor = Color.Red.copy(alpha = 0.7f),
                            contentColor = Color.White,
                        ),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        Icons.Default.SubtitlesOff,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Disable", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (showSubtitleSheet) {
            subtitleBottomSheet(
                availableSubtitles = availableSubtitles,
                currentSubtitle = currentSubtitle,
                currentStyle = subtitleStyle,
                onSubtitleSelected = { track ->
                    viewModel.selectSubtitle(track)
                },
                onStyleChanged = { style ->
                    viewModel.updateSubtitleStyle(style)
                },
                onSearchSubtitles = {
                    isSearching = true
                    viewModel.searchSubtitles(videoUrl, "Big Buck Bunny")
                },
                onDismiss = { viewModel.toggleSubtitleSheet(false) },
                onLoadFromFile = {
                    subtitlePickerLauncher.launch("*/*")
                },
            )
        }
    }
}

@Composable
private fun SubtitleInfoPanel(
    availableSubtitles: List<SubtitleTrack>,
    currentSubtitle: SubtitleTrack?,
    subtitleStyle: SubtitleStyle,
    isSearching: Boolean,
    searchMessage: String,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    onSearchSubtitles: () -> Unit,
    onBrowseFile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .width(360.dp)
                .heightIn(max = 600.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.90f),
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Subtitle Demo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        "Comprehensive Features",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))

            InfoSection(
                title = "Current Status",
                icon = Icons.Default.PlayArrow,
            ) {
                if (isSearching) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            searchMessage,
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else if (currentSubtitle != null) {
                    InfoRow("Active Subtitle", currentSubtitle.language)
                    InfoRow("Format", currentSubtitle.format.name)
                    InfoRow("Source", currentSubtitle.source.name)
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp),
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.Yellow,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "No subtitle active",
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            InfoSection(
                title = "Available Subtitles (${availableSubtitles.size})",
                icon = Icons.Default.List,
            ) {
                if (availableSubtitles.isEmpty()) {
                    Text(
                        "No subtitles loaded yet",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = onSearchSubtitles,
                            modifier = Modifier.weight(1f),
                            colors =
                                ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary,
                                ),
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Search", style = MaterialTheme.typography.bodySmall)
                        }

                        OutlinedButton(
                            onClick = onBrowseFile,
                            modifier = Modifier.weight(1f),
                            colors =
                                ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.secondary,
                                ),
                        ) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Browse", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    availableSubtitles.take(5).forEach { subtitle ->
                        SubtitleListItem(subtitle, currentSubtitle?.id == subtitle.id)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (availableSubtitles.size > 5) {
                        Text(
                            "...and ${availableSubtitles.size - 5} more (open settings to view all)",
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            InfoSection(
                title = "Demo Features",
                icon = Icons.Default.CheckCircle,
            ) {
                FeatureItem("Automatic subtitle search on load", true)
                FeatureItem("Online subtitle download (APIs)", true)
                FeatureItem("Browse & load local subtitle files", true)
                FeatureItem("Format auto-detection (SRT, VTT, ASS, TTML)", true)
                FeatureItem("Customizable styling", true)
                FeatureItem("Multiple language support", true)
                FeatureItem("Subtitle caching", true)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    ),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "How to Use",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Subtitles auto-search on video load\n" +
                            "Click 'Search Online' to find subtitles from internet\n" +
                            "Click 'Browse File' to load from phone storage\n" +
                            "Supported formats: SRT, VTT, ASS, SSA, TTML\n" +
                            "Use 'Settings' for styling options",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Full Settings")
            }
        }
    }
}

@Composable
private fun InfoSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        content()
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            value,
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun SubtitleListItem(
    subtitle: SubtitleTrack,
    isActive: Boolean = false,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isActive) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    } else {
                        Color.White.copy(alpha = 0.1f)
                    },
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Subtitles,
                contentDescription = null,
                tint = if (isActive) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    subtitle.language,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                )
                Text(
                    "${subtitle.source.name} • ${subtitle.format.name}",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (isActive) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Active",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun FeatureItem(
    text: String,
    enabled: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp),
    ) {
        Icon(
            if (enabled) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (enabled) Color.Green else Color.Gray,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text,
            color = Color.White.copy(alpha = if (enabled) 0.9f else 0.5f),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
