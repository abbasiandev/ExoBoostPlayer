package dev.abbasian.exoboostplayer.presentation.ui.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.abbasian.exoboost.domain.model.HighlightConfig
import dev.abbasian.exoboost.domain.model.MediaPlayerConfig
import dev.abbasian.exoboost.domain.model.MediaState
import dev.abbasian.exoboost.domain.model.VideoQuality
import dev.abbasian.exoboost.presentation.ui.screen.exoBoostPlayer
import dev.abbasian.exoboost.presentation.viewmodel.MediaPlayerViewModel
import dev.abbasian.exoboostplayer.presentation.QualityChangeEvent
import org.koin.androidx.compose.koinViewModel

@Composable
fun QualityControlDemo(
    url: String,
    onBack: () -> Unit,
) {
    val viewModel: MediaPlayerViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()

    var qualityChanges by remember { mutableStateOf<List<QualityChangeEvent>>(emptyList()) }
    var previousQuality by remember { mutableStateOf<VideoQuality?>(null) }
    var showQualityPanel by remember { mutableStateOf(true) }

    LaunchedEffect(uiState.mediaInfo.currentQuality) {
        val currentQuality = uiState.mediaInfo.currentQuality
        if (currentQuality != null && currentQuality != previousQuality) {
            qualityChanges = qualityChanges +
                QualityChangeEvent(
                    fromQuality = previousQuality,
                    toQuality = currentQuality,
                    timestamp = System.currentTimeMillis(),
                    reason =
                        when {
                            previousQuality == null -> "Initial quality"
                            previousQuality!!.height > currentQuality.height -> "Auto-downgrade (network issue)"
                            previousQuality!!.height < currentQuality.height -> "Quality upgrade"
                            else -> "Quality change"
                        },
                )
            previousQuality = currentQuality
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        exoBoostPlayer(
            videoUrl = url,
            mediaConfig =
                MediaPlayerConfig(
                    autoPlay = true,
                    showControls = true,
                    retryOnError = true,
                    maxRetryCount = 5,
                    autoQualityOnError = true,
                    enableQualitySelection = true,
                    bufferDurations =
                        MediaPlayerConfig.BufferDurations(
                            minBufferMs = 15000,
                            maxBufferMs = 50000,
                            bufferForPlaybackMs = 2500,
                            bufferForPlaybackAfterRebufferMs = 5000,
                        ),
                    highlightConfig = HighlightConfig.balanced(),
                ),
            onBack = onBack,
            modifier = Modifier.fillMaxSize(),
        )

        if (showQualityPanel) {
            QualityMonitorPanel(
                currentQuality = uiState.mediaInfo.currentQuality,
                availableQualities = uiState.mediaInfo.availableQualities,
                qualityChanges = qualityChanges,
                mediaState = uiState.mediaState,
                onDismiss = { showQualityPanel = false },
            )
        }

        FloatingActionButton(
            onClick = { showQualityPanel = !showQualityPanel },
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
        ) {
            Icon(
                if (showQualityPanel) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription = "Toggle quality monitor",
            )
        }
    }
}

@Composable
private fun QualityMonitorPanel(
    currentQuality: VideoQuality?,
    availableQualities: List<VideoQuality>,
    qualityChanges: List<QualityChangeEvent>,
    mediaState: MediaState,
    onDismiss: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.9f),
            ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Quality Monitor",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            CurrentQualityCard(currentQuality, mediaState)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Available Qualities (${availableQualities.size})",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.7f),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                availableQualities.forEach { quality ->
                    QualityChip(
                        quality = quality,
                        isActive = quality.height == currentQuality?.height,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (qualityChanges.isNotEmpty()) {
                Text(
                    text = "Quality Changes (${qualityChanges.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.7f),
                )
                Spacer(modifier = Modifier.height(8.dp))

                qualityChanges.takeLast(5).reversed().forEach { change ->
                    QualityChangeItem(change)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            InfoCard(
                title = "Auto-Downgrade Feature",
                description =
                    "ExoBoost automatically reduces video quality when network errors occur," +
                        " ensuring smooth playback without buffering.",
            )
        }
    }
}

@Composable
private fun CurrentQualityCard(
    quality: VideoQuality?,
    mediaState: MediaState,
) {
    val backgroundColor =
        when {
            quality?.label == "Auto" -> Color(0xFF2196F3)
            quality != null -> Color(0xFF4CAF50)
            else -> Color.Gray
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(backgroundColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.HighQuality,
            contentDescription = null,
            tint = backgroundColor,
            modifier = Modifier.size(32.dp),
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = "Current Quality",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f),
            )
            Text(
                text = quality?.getQualityLabel() ?: "Loading...",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            if (quality != null && quality.label != "Auto") {
                Text(
                    text = "${quality.width}×${quality.height} • ${quality.bitrate / 1000}kbps",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun QualityChip(
    quality: VideoQuality,
    isActive: Boolean,
) {
    Surface(
        color = if (isActive) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = quality.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isActive) Color.White else Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun QualityChangeItem(change: QualityChangeEvent) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector =
                    when {
                        change.fromQuality == null -> Icons.Filled.PlayArrow
                        change.fromQuality.height > change.toQuality.height -> Icons.Filled.ArrowDownward
                        else -> Icons.Filled.ArrowUpward
                    },
                contentDescription = null,
                tint =
                    when {
                        change.fromQuality == null -> Color(0xFF2196F3)
                        change.fromQuality.height > change.toQuality.height -> Color(0xFFFF9800)
                        else -> Color(0xFF4CAF50)
                    },
                modifier = Modifier.size(20.dp),
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text =
                        if (change.fromQuality != null) {
                            "${change.fromQuality.label} → ${change.toQuality.label}"
                        } else {
                            change.toQuality.label
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                )
                Text(
                    text = change.reason,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    description: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
        }
    }
}
