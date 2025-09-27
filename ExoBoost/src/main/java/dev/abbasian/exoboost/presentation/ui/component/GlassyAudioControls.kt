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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    onVolumeChange: ((Float) -> Unit)? = null,
    volume: Float = 1f,
    trackTitle: String? = null,
    artistName: String? = null,
    onNext: (() -> Unit)? = null,
    onPrevious: (() -> Unit)? = null,
    showEqualizer: Boolean = false,
    onEqualizerChange: ((List<Float>) -> Unit)? = null,
    onEqualizerToggle: (() -> Unit)? = null
) {
    var showVolumeSlider by remember { mutableStateOf(false) }
    var currentVolume by remember { mutableFloatStateOf(volume) }

    GlassyContainer(
        config = config,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(20.dp)
        ) {
            // Track info
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

            if (showEqualizer) {
                GlassyEqualizer(
                    isPlaying = isPlaying,
                    config = config,
                    onEqualizerChange = onEqualizerChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // seek bar
            GlassySeekBar(
                currentPosition = currentPosition,
                bufferedPosition = bufferedPosition,
                duration = duration,
                onSeek = onSeek,
                modifier = Modifier.fillMaxWidth(),
                config = config
            )

            // Main Control Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // previous
                onPrevious?.let { previous ->
                    GlassyControlButton(
                        onClick = previous,
                        icon = Icons.Filled.SkipPrevious,
                        iconSize = 24.dp,
                        buttonSize = 48.dp,
                        description = "Previous"
                    )
                } ?: Box(modifier = Modifier.size(48.dp))

                // play/pause
                GlassyControlButton(
                    onClick = onPlayPause,
                    icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    iconSize = 32.dp,
                    buttonSize = 64.dp,
                    description = if (isPlaying) "Pause" else "Play",
                    isPrimary = true
                )

                // next
                onNext?.let { next ->
                    GlassyControlButton(
                        onClick = next,
                        icon = Icons.Filled.SkipNext,
                        iconSize = 24.dp,
                        buttonSize = 48.dp,
                        description = "Next"
                    )
                } ?: Box(modifier = Modifier.size(48.dp))
            }

            // volume + equalizer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // volume Control
                onVolumeChange?.let { volumeChange ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.clickable {
                                showVolumeSlider = !showVolumeSlider
                            }
                        ) {
                            Icon(
                                Icons.Filled.VolumeUp,
                                contentDescription = "Volume",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )

                            if (showVolumeSlider) {
                                Slider(
                                    value = currentVolume,
                                    onValueChange = { newVolume ->
                                        currentVolume = newVolume
                                        volumeChange(newVolume)
                                    },
                                    valueRange = 0f..1f,
                                    modifier = Modifier.weight(1f),
                                    colors = androidx.compose.material3.SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.White.copy(alpha = 0.8f),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                    )
                                )
                            } else {
                                Text(
                                    text = "${(currentVolume * 100).toInt()}%",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                } ?: Box(modifier = Modifier.weight(1f))

                onEqualizerToggle?.let { equalizerToggle ->
                    GlassyControlButton(
                        onClick = equalizerToggle,
                        icon = Icons.Filled.Equalizer,
                        iconSize = 20.dp,
                        buttonSize = 40.dp,
                        description = if (showEqualizer) "Hide Equalizer" else "Show Equalizer",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GlassyControlButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconSize: androidx.compose.ui.unit.Dp,
    buttonSize: androidx.compose.ui.unit.Dp,
    description: String,
    isPrimary: Boolean = false,
    modifier: Modifier = Modifier
) {
    val alpha = if (isPrimary) 0.4f else 0.3f
    val borderAlpha = if (isPrimary) 0.6f else 0.4f

    Box(
        modifier = modifier
            .size(buttonSize)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isPrimary) 0.25f else 0.2f),
                        Color.White.copy(alpha = if (isPrimary) 0.1f else 0.05f)
                    )
                ),
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = borderAlpha),
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(iconSize),
            tint = Color.White
        )
    }
}