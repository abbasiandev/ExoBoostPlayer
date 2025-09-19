package dev.abbasian.exoboost.presentation.ui.component

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.abbasian.exoboost.domain.model.VideoInfo
import dev.abbasian.exoboost.domain.model.VideoState
import dev.abbasian.exoboost.R

@Composable
fun EnhancedPlayerControls(
    videoState: VideoState,
    videoInfo: VideoInfo,
    showControls: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onRetry: () -> Unit,
    onSettings: (() -> Unit)? = null,
    onFullscreen: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AnimatedVisibility(
        visible = showControls || videoState is VideoState.Error || videoState is VideoState.Loading,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // background gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.4f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )

            when (videoState) {
                is VideoState.Loading -> {
                    EnhancedLoadingIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is VideoState.Error -> {
                    ErrorDisplay(
                        error = videoState.error,
                        onRetry = onRetry,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    // main play/pause button
                    FloatingActionButton(
                        onClick = onPlayPause,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(64.dp),
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        contentColor = Color.White
                    ) {
                        Icon(
                            imageVector = when {
                                videoInfo.isPlaying -> Icons.Filled.Pause
                                videoState is VideoState.Ended -> Icons.Filled.Replay
                                else -> Icons.Filled.PlayArrow
                            },
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // top controls
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        onSettings?.let { settings ->
                            IconButton(
                                onClick = settings,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    Icons.Filled.Settings,
                                    contentDescription = context.getString(R.string.cd_setting),
                                    tint = Color.White
                                )
                            }
                        }

                        onFullscreen?.let { fullscreen ->
                            IconButton(
                                onClick = fullscreen,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    Icons.Filled.Fullscreen,
                                    contentDescription = context.getString(R.string.cd_fullscreen),
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // bottom controls
                    if (videoInfo.duration > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                        ) {
                            EnhancedSeekBar(
                                currentPosition = videoInfo.currentPosition,
                                bufferedPosition = videoInfo.bufferedPosition,
                                duration = videoInfo.duration,
                                onSeek = onSeek,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedLoadingIndicator(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = context.getString(R.string.loading),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}