package dev.abbasian.exoboostplayer.presentation.ui.demo

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.abbasian.exoboost.domain.model.MediaPlayerConfig
import dev.abbasian.exoboost.domain.model.MediaState
import dev.abbasian.exoboost.presentation.ui.screen.exoBoostPlayer
import dev.abbasian.exoboost.presentation.viewmodel.MediaPlayerViewModel
import dev.abbasian.exoboostplayer.presentation.BufferSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.koin.androidx.compose.koinViewModel
import kotlin.math.max

@Composable
fun BufferVisualizationDemo(
    url: String,
    onBack: () -> Unit,
) {
    val viewModel: MediaPlayerViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()

    var bufferHistory by remember { mutableStateOf<List<BufferSnapshot>>(emptyList()) }
    var showBufferPanel by remember { mutableStateOf(true) }
    var rebufferCount by remember { mutableStateOf(0) }
    var wasBuffering by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (isActive) {
            val currentPos = uiState.mediaInfo.currentPosition
            val bufferedPos = uiState.mediaInfo.bufferedPosition
            val duration = uiState.mediaInfo.duration

            if (duration > 0) {
                val bufferAhead = bufferedPos - currentPos
                val bufferPercentage = (bufferAhead.toFloat() / duration * 100).coerceIn(0f, 100f)

                bufferHistory =
                    (
                        bufferHistory +
                            BufferSnapshot(
                                timestamp = System.currentTimeMillis(),
                                bufferedPosition = bufferedPos,
                                currentPosition = currentPos,
                                bufferPercentage = bufferPercentage,
                            )
                    ).takeLast(100)
            }

            val isBuffering = uiState.mediaState is MediaState.Loading && currentPos > 0
            if (isBuffering && !wasBuffering) {
                rebufferCount++
            }
            wasBuffering = isBuffering

            delay(500)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        exoBoostPlayer(
            videoUrl = url,
            mediaConfig =
                MediaPlayerConfig(
                    autoPlay = true,
                    showControls = true,
                    bufferDurations =
                        MediaPlayerConfig.BufferDurations(
                            minBufferMs = 20000,
                            maxBufferMs = 60000,
                            bufferForPlaybackMs = 3000,
                            bufferForPlaybackAfterRebufferMs = 6000,
                        ),
                ),
            onBack = onBack,
            modifier = Modifier.fillMaxSize(),
        )

        if (showBufferPanel) {
            BufferHealthPanel(
                bufferHistory = bufferHistory,
                mediaInfo = uiState.mediaInfo,
                mediaState = uiState.mediaState,
                rebufferCount = rebufferCount,
                onDismiss = { showBufferPanel = false },
            )
        }

        FloatingActionButton(
            onClick = { showBufferPanel = !showBufferPanel },
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
        ) {
            Icon(
                if (showBufferPanel) Icons.Filled.VisibilityOff else Icons.Filled.DataUsage,
                contentDescription = "Toggle buffer monitor",
            )
        }
    }
}

@Composable
private fun BufferHealthPanel(
    bufferHistory: List<BufferSnapshot>,
    mediaInfo: dev.abbasian.exoboost.domain.model.MediaInfo,
    mediaState: MediaState,
    rebufferCount: Int,
    onDismiss: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.95f),
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
                    text = "Buffer Health",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            CurrentBufferStatus(mediaInfo, mediaState)

            Spacer(modifier = Modifier.height(16.dp))

            if (bufferHistory.isNotEmpty()) {
                Text(
                    text = "Buffer History (Last 50s)",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f),
                )
                Spacer(modifier = Modifier.height(8.dp))
                BufferGraph(bufferHistory)
            }

            Spacer(modifier = Modifier.height(16.dp))

            BufferStatistics(
                bufferHistory = bufferHistory,
                rebufferCount = rebufferCount,
            )

            Spacer(modifier = Modifier.height(16.dp))

            BufferConfigInfo()
        }
    }
}

@Composable
private fun CurrentBufferStatus(
    mediaInfo: dev.abbasian.exoboost.domain.model.MediaInfo,
    mediaState: MediaState,
) {
    val bufferAhead = max(0, mediaInfo.bufferedPosition - mediaInfo.currentPosition)
    val bufferSeconds = bufferAhead / 1000

    val bufferHealth =
        when {
            bufferSeconds > 10 -> Triple(Color(0xFF4CAF50), "Excellent", Icons.Filled.CheckCircle)
            bufferSeconds > 5 -> Triple(Color(0xFFFF9800), "Good", Icons.Filled.Check)
            bufferSeconds > 2 -> Triple(Color(0xFFFF9800), "Low", Icons.Filled.Warning)
            else -> Triple(Color(0xFFF44336), "Critical", Icons.Filled.Error)
        }

    Surface(
        color = bufferHealth.first.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = bufferHealth.third,
                contentDescription = null,
                tint = bufferHealth.first,
                modifier = Modifier.size(32.dp),
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "Buffer: ${bufferSeconds}s",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Health: ${bufferHealth.second}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun BufferGraph(bufferHistory: List<BufferSnapshot>) {
    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                .padding(8.dp),
    ) {
        if (bufferHistory.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val pointSpacing = width / maxOf(1f, (bufferHistory.size - 1).toFloat())

        for (i in 0..4) {
            val y = height * i / 4
            drawLine(
                color = Color.White.copy(alpha = 0.1f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f,
            )
        }

        val path = Path()
        bufferHistory.forEachIndexed { index, snapshot ->
            val x = index * pointSpacing
            val y = height - (snapshot.bufferPercentage / 100f * height)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = Color(0xFF4CAF50),
            style = Stroke(width = 3f),
        )

        val fillPath =
            Path().apply {
                moveTo(0f, height)
                bufferHistory.forEachIndexed { index, snapshot ->
                    val x = index * pointSpacing
                    val y = height - (snapshot.bufferPercentage / 100f * height)
                    lineTo(x, y)
                }
                lineTo((bufferHistory.size - 1) * pointSpacing, height)
                close()
            }

        drawPath(
            path = fillPath,
            color = Color(0xFF4CAF50).copy(alpha = 0.2f),
        )

        bufferHistory.forEachIndexed { index, snapshot ->
            val x = index * pointSpacing
            val y = height - (snapshot.bufferPercentage / 100f * height)
            drawCircle(
                color = Color(0xFF4CAF50),
                radius = 3f,
                center = Offset(x, y),
            )
        }
    }
}

@Composable
private fun BufferStatistics(
    bufferHistory: List<BufferSnapshot>,
    rebufferCount: Int,
) {
    if (bufferHistory.isEmpty()) return

    val avgBuffer = bufferHistory.map { it.bufferPercentage }.average().toFloat()
    val minBuffer = bufferHistory.minOfOrNull { it.bufferPercentage } ?: 0f
    val maxBuffer = bufferHistory.maxOfOrNull { it.bufferPercentage } ?: 0f

    Column {
        Text(
            text = "Statistics",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.7f),
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatItem("Average", "${avgBuffer.toInt()}%")
            StatItem("Min", "${minBuffer.toInt()}%")
            StatItem("Max", "${maxBuffer.toInt()}%")
            StatItem("Rebuffers", rebufferCount.toString())
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun BufferConfigInfo() {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Buffer Configuration",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Min: 20s | Max: 60s | Playback: 3s | Rebuffer: 6s",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}
