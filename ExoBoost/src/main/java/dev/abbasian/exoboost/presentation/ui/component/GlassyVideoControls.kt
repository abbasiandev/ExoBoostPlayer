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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.abbasian.exoboost.R
import dev.abbasian.exoboost.domain.model.MediaInfo
import dev.abbasian.exoboost.domain.model.MediaPlayerConfig
import dev.abbasian.exoboost.domain.model.MediaState
import dev.abbasian.exoboost.domain.model.VideoQuality

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun enhancedPlayerControls(
    mediaState: MediaState,
    mediaInfo: MediaInfo,
    showControls: Boolean,
    mediaConfig: MediaPlayerConfig = MediaPlayerConfig(),
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onRetry: () -> Unit,
    onSpeedSelected: (Float) -> Unit = {},
    onQualitySelected: (VideoQuality) -> Unit = {},
    onSettings: (() -> Unit)? = null,
    onFullscreen: (() -> Unit)? = null,
    onModalStateChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val glassyConfig: MediaPlayerConfig.GlassyUIConfig = MediaPlayerConfig.GlassyUIConfig()
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }

    LaunchedEffect(showSpeedDialog, showQualityDialog) {
        onModalStateChanged(showSpeedDialog || showQualityDialog)
    }

    AnimatedVisibility(
        visible = showControls || mediaState is MediaState.Error || mediaState is MediaState.Loading,
        enter = fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.8f),
        exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.8f),
        modifier = modifier,
    ) {
        glassyContainer(
            config = glassyConfig,
            modifier = modifier,
        ) {
            when (mediaState) {
                is MediaState.Loading -> {
                    glassyLoadingIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                is MediaState.Error -> {
                    errorDisplay(
                        error = mediaState.error,
                        onRetry = onRetry,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                else -> {
                    // main play/pause button
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .size(80.dp)
                                .clip(RoundedCornerShape(40.dp))
                                .background(
                                    brush =
                                        Brush.radialGradient(
                                            colors =
                                                listOf(
                                                    Color.White.copy(alpha = 0.15f),
                                                    Color.White.copy(alpha = 0.05f),
                                                ),
                                        ),
                                ).border(
                                    width = 1.dp,
                                    brush =
                                        Brush.linearGradient(
                                            colors =
                                                listOf(
                                                    Color.White.copy(alpha = 0.3f),
                                                    Color.White.copy(alpha = 0.1f),
                                                ),
                                        ),
                                    shape = RoundedCornerShape(40.dp),
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        IconButton(
                            onClick = onPlayPause,
                            modifier = Modifier.size(64.dp),
                        ) {
                            Icon(
                                imageVector =
                                    when {
                                        mediaInfo.isPlaying -> Icons.Filled.Pause
                                        mediaState is MediaState.Ended -> Icons.Filled.Replay
                                        else -> Icons.Filled.PlayArrow
                                    },
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }

                    // top controls
                    Row(
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    brush =
                                        Brush.linearGradient(
                                            colors =
                                                listOf(
                                                    Color.White.copy(alpha = 0.1f),
                                                    Color.White.copy(alpha = 0.05f),
                                                ),
                                        ),
                                ).border(
                                    width = 0.5.dp,
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(16.dp),
                                ).padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (mediaConfig.enableSpeedControl) {
                            glassyIconButton(
                                onClick = { showSpeedDialog = true },
                                icon = Icons.Filled.Speed,
                                contentDescription = context.getString(R.string.cd_speed_control),
                            )
                        }

                        if (mediaConfig.enableQualitySelection && mediaInfo.availableQualities.isNotEmpty()) {
                            glassyIconButton(
                                onClick = { showQualityDialog = true },
                                icon = Icons.Filled.HighQuality,
                                contentDescription = context.getString(R.string.cd_quality_selection),
                            )
                        }

                        onSettings?.let { settings ->
                            glassyIconButton(
                                onClick = settings,
                                icon = Icons.Filled.Settings,
                                contentDescription = context.getString(R.string.cd_setting),
                            )
                        }

                        onFullscreen?.let { fullscreen ->
                            glassyIconButton(
                                onClick = fullscreen,
                                icon = Icons.Filled.Fullscreen,
                                contentDescription = context.getString(R.string.cd_fullscreen),
                            )
                        }
                    }

                    // bottom controls
                    if (mediaInfo.duration > 0) {
                        Box(
                            modifier =
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        brush =
                                            Brush.radialGradient(
                                                colors =
                                                    listOf(
                                                        Color.White.copy(alpha = 0.05f),
                                                        Color.Transparent,
                                                    ),
                                            ),
                                    ).border(
                                        width = 1.dp,
                                        brush =
                                            Brush.linearGradient(
                                                colors =
                                                    listOf(
                                                        Color.White.copy(alpha = 0.2f),
                                                        Color.White.copy(alpha = 0.1f),
                                                    ),
                                            ),
                                        shape = RoundedCornerShape(16.dp),
                                    ).padding(16.dp),
                        ) {
                            glassySeekBar(
                                currentPosition = mediaInfo.currentPosition,
                                bufferedPosition = mediaInfo.bufferedPosition,
                                duration = mediaInfo.duration,
                                onSeek = onSeek,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                            )
                        }
                    }

                    if (showSpeedDialog) {
                        ModalBottomSheet(
                            onDismissRequest = { showSpeedDialog = false },
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            dragHandle = null,
                        ) {
                            speedControlBottomSheet(
                                currentSpeed = mediaInfo.playbackSpeed,
                                availableSpeeds = mediaConfig.playbackSpeedOptions,
                                onSpeedSelected = onSpeedSelected,
                                onDismiss = { showSpeedDialog = false },
                            )
                        }
                    }

                    if (showQualityDialog) {
                        ModalBottomSheet(
                            onDismissRequest = { showQualityDialog = false },
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            dragHandle = null,
                        ) {
                            qualitySelectionBottomSheet(
                                availableQualities = mediaInfo.availableQualities,
                                currentQuality = mediaInfo.currentQuality,
                                onQualitySelected = onQualitySelected,
                                onDismiss = { showQualityDialog = false },
                            )
                        }
                    }
                }
            }
        }
    }
}
