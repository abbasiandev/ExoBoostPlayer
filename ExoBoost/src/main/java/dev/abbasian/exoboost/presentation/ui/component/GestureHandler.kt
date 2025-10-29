package dev.abbasian.exoboost.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abbasian.exoboost.util.formatTime
import kotlinx.coroutines.delay

@Composable
fun gestureHandler(
    volume: Float,
    brightness: Float,
    onVolumeChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onSeek: (Long) -> Unit,
    currentPosition: Long,
    duration: Long,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current

    var gestureState by remember { mutableStateOf(GestureState.None) }
    var showGestureIndicator by remember { mutableStateOf(false) }
    var gestureValue by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(showGestureIndicator) {
        if (showGestureIndicator) {
            delay(1500)
            showGestureIndicator = false
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val x = offset.x
                            val y = offset.y
                            val centerX = size.width / 2f

                            gestureState =
                                when {
                                    x < centerX / 2 -> GestureState.Brightness
                                    x > centerX + centerX / 2 -> GestureState.Volume
                                    else -> GestureState.Seek
                                }

                            gestureValue =
                                when (gestureState) {
                                    GestureState.Volume -> volume
                                    GestureState.Brightness -> brightness
                                    GestureState.Seek -> currentPosition.toFloat() / duration.toFloat()
                                    else -> 0f
                                }

                            showGestureIndicator = true
                        },
                        onDragEnd = {
                            gestureState = GestureState.None
                        },
                    ) { _, dragAmount ->
                        val sensitivity = 0.003f
                        val deltaY = -dragAmount.y * sensitivity

                        when (gestureState) {
                            GestureState.Volume -> {
                                val newVolume = (gestureValue + deltaY).coerceIn(0f, 1f)
                                gestureValue = newVolume
                                onVolumeChange(newVolume)
                            }

                            GestureState.Brightness -> {
                                val newBrightness = (gestureValue + deltaY).coerceIn(0f, 1f)
                                gestureValue = newBrightness
                                onBrightnessChange(newBrightness)
                            }

                            GestureState.Seek -> {
                                if (duration > 0) {
                                    val newProgress =
                                        (gestureValue + deltaY * 0.1f).coerceIn(0f, 1f)
                                    gestureValue = newProgress
                                    val newPosition = (newProgress * duration).toLong()
                                    onSeek(newPosition)
                                }
                            }

                            else -> {}
                        }
                    }
                },
    ) {
        // Gesture indicators
        if (showGestureIndicator) {
            when (gestureState) {
                GestureState.Volume -> {
                    volumeGestureIndicator(
                        volume = gestureValue,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                }

                GestureState.Brightness -> {
                    brightnessGestureIndicator(
                        brightness = gestureValue,
                        modifier = Modifier.align(Alignment.CenterStart),
                    )
                }

                GestureState.Seek -> {
                    seekGestureIndicator(
                        position = (gestureValue * duration).toLong(),
                        duration = duration,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                else -> {}
            }
        }
    }
}

private enum class GestureState {
    None,
    Volume,
    Brightness,
    Seek,
}

@Composable
private fun volumeGestureIndicator(
    volume: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .padding(32.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    brush =
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    Color.White.copy(alpha = 0.15f),
                                    Color.White.copy(alpha = 0.05f),
                                ),
                        ),
                )
                .border(
                    width = 1.dp,
                    brush =
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    Color.White.copy(alpha = 0.3f),
                                    Color.White.copy(alpha = 0.1f),
                                ),
                        ),
                    shape = RoundedCornerShape(20.dp),
                )
                .padding(24.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector =
                    when {
                        volume == 0f -> Icons.Filled.VolumeOff
                        volume < 0.5f -> Icons.Filled.VolumeDown
                        else -> Icons.Filled.VolumeUp
                    },
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(32.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // glass progress bar
            Box(
                modifier =
                    Modifier
                        .width(80.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(volume)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                brush =
                                    Brush.linearGradient(
                                        colors =
                                            listOf(
                                                Color(0xFF4C8AFF).copy(alpha = 0.9f),
                                                Color(0xFF4C8AFF).copy(alpha = 0.7f),
                                            ),
                                    ),
                            ),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "${(volume * 100).toInt()}%",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun brightnessGestureIndicator(
    brightness: Float,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.padding(32.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.8f),
            ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector =
                    when {
                        brightness < 0.3f -> Icons.Filled.BrightnessLow
                        brightness < 0.7f -> Icons.Filled.BrightnessMedium
                        else -> Icons.Filled.BrightnessHigh
                    },
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // brightness progress bar
            LinearProgressIndicator(
                progress = brightness,
                modifier =
                    Modifier
                        .width(60.dp)
                        .height(4.dp),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${(brightness * 100).toInt()}%",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun seekGestureIndicator(
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.8f),
            ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.FastForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "${position.formatTime()} / ${duration.formatTime()}",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
