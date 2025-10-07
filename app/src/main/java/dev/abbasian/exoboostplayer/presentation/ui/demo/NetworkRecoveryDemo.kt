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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.abbasian.exoboost.domain.model.MediaPlayerConfig
import dev.abbasian.exoboost.domain.model.MediaState
import dev.abbasian.exoboost.presentation.ui.screen.ExoBoostPlayer
import dev.abbasian.exoboost.presentation.viewmodel.MediaPlayerViewModel
import dev.abbasian.exoboostplayer.presentation.RetryAttempt
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import kotlin.time.Duration.Companion.seconds

@Composable
fun NetworkRecoveryDemo(url: String, onBack: () -> Unit) {
    val viewModel: MediaPlayerViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()

    var retryAttempts by remember { mutableStateOf<List<RetryAttempt>>(emptyList()) }
    var currentAttempt by remember { mutableStateOf(0) }
    var showMetrics by remember { mutableStateOf(true) }

    LaunchedEffect(uiState.mediaState) {
        when (val state = uiState.mediaState) {
            is MediaState.Error -> {
                currentAttempt++
                val backoffDelay = (1000L * (1 shl (currentAttempt - 1))).coerceAtMost(10000L)
                retryAttempts = retryAttempts + RetryAttempt(
                    attemptNumber = currentAttempt,
                    timestamp = System.currentTimeMillis(),
                    delayMs = backoffDelay,
                    success = false
                )
            }

            is MediaState.Ready, is MediaState.Playing -> {
                if (currentAttempt > 0) {
                    retryAttempts = retryAttempts.dropLast(1) + RetryAttempt(
                        attemptNumber = currentAttempt,
                        timestamp = System.currentTimeMillis(),
                        delayMs = 0,
                        success = true
                    )
                    delay(2.seconds)
                    currentAttempt = 0
                }
            }

            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ExoBoostPlayer(
            videoUrl = url,
            mediaConfig = MediaPlayerConfig(
                autoPlay = true,
                showControls = true,
                retryOnError = true,
                maxRetryCount = 5,
                bufferDurations = MediaPlayerConfig.BufferDurations(
                    minBufferMs = 20000,
                    maxBufferMs = 60000,
                    bufferForPlaybackMs = 3000,
                    bufferForPlaybackAfterRebufferMs = 6000
                )
            ),
            onBack = onBack,
            modifier = Modifier.fillMaxSize()
        )

        if (showMetrics) {
            RetryVisualizationOverlay(
                retryAttempts = retryAttempts,
                currentState = uiState.mediaState,
                onDismiss = { showMetrics = false }
            )
        }

        FloatingActionButton(
            onClick = { showMetrics = !showMetrics },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                if (showMetrics) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription = "Toggle metrics"
            )
        }
    }
}

@Composable
private fun RetryVisualizationOverlay(
    retryAttempts: List<RetryAttempt>,
    currentState: MediaState,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recovery Timeline",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            StateIndicator(currentState)

            Spacer(modifier = Modifier.height(16.dp))

            if (retryAttempts.isNotEmpty()) {
                Text(
                    text = "Retry Attempts (${retryAttempts.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(12.dp))

                retryAttempts.forEach { attempt ->
                    RetryAttemptItem(attempt)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                BackoffVisualization(retryAttempts.filter { !it.success })

            } else {
                Text(
                    text = "No retry attempts yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun StateIndicator(state: MediaState) {
    val (icon, color, text) = when (state) {
        is MediaState.Loading -> Triple(Icons.Filled.CloudSync, Color(0xFFFF9800), "Connecting...")
        is MediaState.Ready, is MediaState.Playing -> Triple(
            Icons.Filled.CheckCircle,
            Color(0xFF4CAF50),
            "Connected"
        )

        is MediaState.Error -> Triple(
            Icons.Filled.Error,
            Color(0xFFF44336),
            "Error: ${state.error.message.take(50)}"
        )

        is MediaState.Ended -> Triple(Icons.Filled.Stop, Color.Gray, "Ended")
        else -> Triple(Icons.Filled.Info, Color.Gray, "Idle")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
}

@Composable
private fun RetryAttemptItem(attempt: RetryAttempt) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (attempt.success) Icons.Filled.CheckCircle else Icons.Filled.Refresh,
                contentDescription = null,
                tint = if (attempt.success) Color(0xFF4CAF50) else Color(0xFFFF9800),
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Attempt #${attempt.attemptNumber}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
        if (!attempt.success && attempt.delayMs > 0) {
            Text(
                text = "Backoff: ${attempt.delayMs}ms",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun BackoffVisualization(failedAttempts: List<RetryAttempt>) {
    if (failedAttempts.isEmpty()) return

    Column {
        Text(
            text = "Exponential Backoff Pattern",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            val maxDelay = failedAttempts.maxOfOrNull { it.delayMs } ?: 1L
            val width = size.width
            val height = size.height
            val barWidth = width / failedAttempts.size

            failedAttempts.forEachIndexed { index, attempt ->
                val barHeight = (attempt.delayMs.toFloat() / maxDelay) * height * 0.8f
                drawRect(
                    color = Color(0xFFFF9800),
                    topLeft = Offset(index * barWidth + 4, height - barHeight),
                    size = androidx.compose.ui.geometry.Size(barWidth - 8, barHeight)
                )
            }
        }
    }
}