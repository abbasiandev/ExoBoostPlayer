package dev.abbasian.exoboost.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abbasian.exoboost.util.formatTime

@Composable
fun EnhancedSeekBar(
    currentPosition: Long,
    bufferedPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    onSeekStart: () -> Unit = {},
    onSeekEnd: () -> Unit = {},
    modifier: Modifier = Modifier
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

    Column(
        modifier = modifier.padding(horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Buffer indicator background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            )

            // Buffered portion
            Box(
                modifier = Modifier
                    .fillMaxWidth(bufferedProgress.coerceIn(0f, 1f))
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.5f))
                    .align(Alignment.CenterStart)
            )

            // Seek slider
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
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = Color.Transparent, // We use our custom background
                    inactiveTrackColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Time display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isSeeking) (sliderPosition * duration).toLong().formatTime()
                else currentPosition.formatTime(),
                color = Color.White,
                fontSize = 12.sp,
                style = MaterialTheme.typography.labelSmall
            )

            if (duration > 0) {
                Text(
                    text = duration.formatTime(),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}