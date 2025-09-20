package dev.abbasian.exoboost.presentation.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.abbasian.exoboost.R
import dev.abbasian.exoboost.domain.model.VideoInfo
import dev.abbasian.exoboost.domain.model.VideoState

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
        enter = fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.8f),
        exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.8f),
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
                                Color.Black.copy(alpha = 0.6f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
            )

            when (videoState) {
                is VideoState.Loading -> {
                    GlassyLoadingIndicator(
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
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(80.dp)
                            .clip(RoundedCornerShape(40.dp))
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.15f),
                                        Color.White.copy(alpha = 0.05f)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.3f),
                                        Color.White.copy(alpha = 0.1f)
                                    )
                                ),
                                shape = RoundedCornerShape(40.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onPlayPause,
                            modifier = Modifier.size(64.dp)
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
                    }

                    // top controls
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.1f),
                                        Color.White.copy(alpha = 0.05f)
                                    )
                                )
                            )
                            .border(
                                width = 0.5.dp,
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        onSettings?.let { settings ->
                            GlassyIconButton(
                                onClick = settings,
                                icon = Icons.Filled.Settings,
                                contentDescription = context.getString(R.string.cd_setting)
                            )
                        }

                        onFullscreen?.let { fullscreen ->
                            GlassyIconButton(
                                onClick = fullscreen,
                                icon = Icons.Filled.Fullscreen,
                                contentDescription = context.getString(R.string.cd_fullscreen)
                            )
                        }
                    }

                    // bottom controls
                    if (videoInfo.duration > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.05f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.2f),
                                            Color.White.copy(alpha = 0.1f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp)
                        ) {
                            GlassySeekBar(
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