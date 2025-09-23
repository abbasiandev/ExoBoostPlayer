package dev.abbasian.exoboost.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abbasian.exoboost.domain.model.VideoPlayerConfig

@Composable
fun GlassyAudioControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    currentPosition: Long,
    duration: Long,
    bufferedPosition: Long = 0L,
    modifier: Modifier = Modifier,
    config: VideoPlayerConfig.GlassyUIConfig = VideoPlayerConfig.GlassyUIConfig(),
    onSkipPrevious: (() -> Unit)? = null,
    onSkipNext: (() -> Unit)? = null,
    onVolumeChange: ((Float) -> Unit)? = null,
    onRepeatToggle: (() -> Unit)? = null,
    onShuffleToggle: (() -> Unit)? = null,
    volume: Float = 1f,
    isRepeatEnabled: Boolean = false,
    isShuffleEnabled: Boolean = false,
    trackTitle: String? = null,
    artistName: String? = null
) {
    GlassyContainer(
        config = config,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            if (trackTitle != null || artistName != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    trackTitle?.let { title ->
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    artistName?.let { artist ->
                        Text(
                            text = artist,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            GlassySeekBar(
                currentPosition = currentPosition,
                bufferedPosition = bufferedPosition,
                duration = duration,
                onSeek = onSeek,
                modifier = Modifier.fillMaxWidth(),
                config = config
            )

            // time Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(currentPosition),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
                Text(
                    text = formatTime(duration),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }

            // Main Control Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous Track
                onSkipPrevious?.let { skipPrevious ->
                    GlassyControlButton(
                        icon = Icons.Filled.FastRewind,
                        onClick = skipPrevious,
                        size = 48.dp,
                        iconSize = 24.dp
                    )
                }

                // Main Play/Pause Button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.25f),
                                    Color.White.copy(alpha = 0.1f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                        .clickable { onPlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(36.dp),
                        tint = Color.White
                    )
                }

                // Next Track
                onSkipNext?.let { skipNext ->
                    GlassyControlButton(
                        icon = Icons.Filled.FastForward,
                        onClick = skipNext,
                        size = 48.dp,
                        iconSize = 24.dp
                    )
                }
            }

            // Secondary Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                onShuffleToggle?.let { shuffleToggle ->
                    GlassyControlButton(
                        icon = Icons.Filled.Shuffle,
                        onClick = shuffleToggle,
                        size = 40.dp,
                        iconSize = 20.dp,
                        isActive = isShuffleEnabled
                    )
                }

                // Repeat
                onRepeatToggle?.let { repeatToggle ->
                    GlassyControlButton(
                        icon = Icons.Filled.Repeat,
                        onClick = repeatToggle,
                        size = 40.dp,
                        iconSize = 20.dp,
                        isActive = isRepeatEnabled
                    )
                }

                // Volume Control
                onVolumeChange?.let { volumeChange ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.VolumeUp,
                            contentDescription = "Volume",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )

                        // Simple volume slider representation
                        GlassyVolumeSlider(
                            volume = volume,
                            onVolumeChange = volumeChange,
                            modifier = Modifier.width(80.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassyControlButton(
    icon: ImageVector,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    isActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .background(
                brush = Brush.radialGradient(
                    colors = if (isActive) {
                        listOf(
                            Color.White.copy(alpha = 0.3f),
                            Color.White.copy(alpha = 0.15f)
                        )
                    } else {
                        listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    }
                ),
                shape = CircleShape
            )
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = if (isActive) 0.4f else 0.2f),
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = Color.White.copy(alpha = if (isActive) 1f else 0.8f)
        )
    }
}

@Composable
private fun GlassyVolumeSlider(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(4.dp)
            )
            .border(
                0.5.dp,
                Color.White.copy(alpha = 0.2f),
                RoundedCornerShape(4.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(volume)
                .background(
                    Color.White.copy(alpha = 0.6f),
                    RoundedCornerShape(4.dp)
                )
                .padding(vertical = 4.dp)
        )
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
