package dev.abbasian.exoboost.presentation.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abbasian.exoboost.domain.model.VideoPlayerConfig
import dev.abbasian.exoboost.util.formatTime

@Composable
fun GlassySeekBar(
    currentPosition: Long,
    bufferedPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    onSeekStart: () -> Unit = {},
    onSeekEnd: () -> Unit = {},
    modifier: Modifier = Modifier,
    config: VideoPlayerConfig.GlassyUIConfig = VideoPlayerConfig.GlassyUIConfig()
) {
    var isSeeking by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }

    val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
    val bufferedProgress = if (duration > 0) bufferedPosition.toFloat() / duration else 0f

    LaunchedEffect(currentPosition) {
        if (!isSeeking) {
            sliderPosition = progress
        }
    }

    GlassyContainer(
        modifier = modifier,
        config = config,
        contentPadding = 16.dp
    ) {
        Column(
            modifier = modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                contentAlignment = Alignment.Center
            ) {
                // Integrated glassy buffer and seek track
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    // Background track
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.1f),
                        topLeft = Offset.Zero,
                        size = Size(canvasWidth, canvasHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                    )

                    // Buffered portion
                    if (bufferedProgress > 0f) {
                        drawRoundRect(
                            color = Color(0xFF4C8AFF).copy(alpha = 0.3f),
                            topLeft = Offset.Zero,
                            size = Size(canvasWidth * bufferedProgress, canvasHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )
                    }

                    // Current position
                    if (progress > 0f) {
                        drawRoundRect(
                            color = Color(0xFF4C8AFF).copy(alpha = 0.8f),
                            topLeft = Offset.Zero,
                            size = Size(canvasWidth * progress, canvasHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )
                    }

                    // Glass highlight
                    if (progress > 0f) {
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.2f),
                            topLeft = Offset(0f, 0f),
                            size = Size(canvasWidth * progress, canvasHeight * 0.3f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )
                    }
                }

                // slider for touch handling
                Slider(
                    value = sliderPosition,
                    onValueChange = {
                        sliderPosition = it
                        if (!isSeeking) {
                            isSeeking = true
                            onSeekStart()
                        }
                    },
                    onValueChangeFinished = {
                        val newPosition = (sliderPosition * duration).toLong()
                        onSeek(newPosition)
                        isSeeking = false
                        onSeekEnd()
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF4C8AFF).copy(alpha = 0.9f),
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Time display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSeeking) (sliderPosition * duration).toLong().formatTime()
                    else currentPosition.formatTime(),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    style = MaterialTheme.typography.labelMedium
                )

                if (duration > 0) {
                    Text(
                        text = duration.formatTime(),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}