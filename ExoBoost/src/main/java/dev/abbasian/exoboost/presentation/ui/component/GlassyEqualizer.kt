package dev.abbasian.exoboost.presentation.ui.component

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abbasian.exoboost.domain.model.VideoPlayerConfig

@Composable
fun GlassyEqualizer(
    modifier: Modifier = Modifier,
    config: VideoPlayerConfig.GlassyUIConfig = VideoPlayerConfig.GlassyUIConfig(),
    barCount: Int = 8,
    isPlaying: Boolean = true,
    onEqualizerChange: ((List<Float>) -> Unit)? = null
) {
    val frequencyLabels = listOf("60Hz", "170Hz", "310Hz", "600Hz", "1kHz", "3kHz", "6kHz", "12kHz")

    val equalizerValues = remember {
        mutableStateListOf<Float>().apply {
            repeat(barCount) { add(0.5f) }
        }
    }

    GlassyContainer(
        config = config.copy(
            backgroundOpacity = config.backgroundOpacity * 0.8f
        ),
        contentPadding = 16.dp,
        modifier = modifier
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Equalizer",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(barCount) { index ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // frequency label
                        Text(
                            text = frequencyLabels.getOrElse(index) { "${index + 1}" },
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            maxLines = 1
                        )

                        // vertical slider
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(120.dp)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.1f),
                                            Color.White.copy(alpha = 0.05f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .border(
                                    width = 0.5.dp,
                                    color = Color.White.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(20.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            VerticalSlider(
                                value = equalizerValues[index],
                                onValueChange = { newValue ->
                                    equalizerValues[index] = newValue
                                    onEqualizerChange?.invoke(equalizerValues.toList())
                                },
                                valueRange = 0f..1f,
                                modifier = Modifier
                                    .height(100.dp)
                                    .width(20.dp)
                            )
                        }

                        // dB
                        val dbValue = ((equalizerValues[index] - 0.5f) * 24).toInt()
                        Text(
                            text = "${if (dbValue > 0) "+" else ""}${dbValue}dB",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 9.sp
                        )
                    }
                }
            }

            // presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Flat", "Rock", "Pop", "Jazz").forEach { preset ->
                    GlassyPresetButton(
                        text = preset,
                        onClick = {
                            val presetValues = getPresetValues(preset)
                            presetValues.forEachIndexed { index, value ->
                                if (index < equalizerValues.size) {
                                    equalizerValues[index] = value
                                }
                            }
                            onEqualizerChange?.invoke(equalizerValues.toList())
                        }
                    )
                }
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                    },
                    onDragEnd = {
                        isDragging = false
                    }
                ) { _, dragAmount ->
                    val heightPx = size.height.toFloat()
                    val deltaValue =
                        -dragAmount.y / heightPx * (valueRange.endInclusive - valueRange.start)
                    val newValue =
                        (value + deltaValue).coerceIn(valueRange.start, valueRange.endInclusive)
                    onValueChange(newValue)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val heightPx = size.height.toFloat()
                    val normalizedY = (heightPx - offset.y) / heightPx
                    val newValue =
                        (valueRange.start + normalizedY * (valueRange.endInclusive - valueRange.start))
                            .coerceIn(valueRange.start, valueRange.endInclusive)
                    onValueChange(newValue)
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.White.copy(alpha = 0.15f),
                    RoundedCornerShape(10.dp)
                )
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.3f),
                    RoundedCornerShape(10.dp)
                )
        )

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val normalizedValue =
                (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
            val thumbY = maxHeight * (1f - normalizedValue)
            val activeHeight = maxHeight - thumbY

            if (activeHeight > 0.dp) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .width(maxWidth)
                        .height(activeHeight)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.6f),
                                    Color.White.copy(alpha = 0.3f)
                                )
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                )
            }

            // thumb
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = thumbY - 8.dp)
                    .size(24.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDragging) 1f else 0.9f),
                                Color.White.copy(alpha = if (isDragging) 0.8f else 0.7f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .border(
                        width = if (isDragging) 2.dp else 1.dp,
                        color = Color.White.copy(alpha = if (isDragging) 1f else 0.8f),
                        shape = CircleShape
                    )
                    .shadow(
                        elevation = if (isDragging) 8.dp else 4.dp,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun GlassyPresetButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 12.sp
        )
    }
}

private fun getPresetValues(preset: String): List<Float> {
    return when (preset) {
        "Flat" -> listOf(0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f)
        "Rock" -> listOf(0.6f, 0.5f, 0.4f, 0.4f, 0.5f, 0.7f, 0.8f, 0.8f)
        "Pop" -> listOf(0.4f, 0.6f, 0.7f, 0.7f, 0.5f, 0.4f, 0.5f, 0.6f)
        "Jazz" -> listOf(0.7f, 0.6f, 0.5f, 0.4f, 0.4f, 0.5f, 0.6f, 0.7f)
        else -> listOf(0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f)
    }
}