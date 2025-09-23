package dev.abbasian.exoboost.presentation.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.abbasian.exoboost.R
import dev.abbasian.exoboost.domain.model.VideoPlayerConfig

@Composable
fun SpeedControlBottomSheet(
    currentSpeed: Float,
    availableSpeeds: List<Float>,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit,
    config: VideoPlayerConfig.GlassyUIConfig = VideoPlayerConfig.GlassyUIConfig()
) {
    val context = LocalContext.current

    GlassyContainer(
        modifier = Modifier.fillMaxWidth(),
        config = config,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column {
            // header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = context.getString(R.string.playback_speed),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // speed options
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(availableSpeeds) { speed ->
                    SpeedChip(
                        speed = speed,
                        isSelected = speed == currentSpeed,
                        onClick = {
                            onSpeedSelected(speed)
                            onDismiss()
                        },
                        config = config
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // current speed indicator
            GlassyContainer(
                config = config.copy(backgroundOpacity = 0.15f),
                shape = RoundedCornerShape(12.dp),
                contentPadding = 16.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = context.getString(R.string.current_speed),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${currentSpeed}x",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedChip(
    speed: Float,
    isSelected: Boolean,
    onClick: () -> Unit,
    config: VideoPlayerConfig.GlassyUIConfig = VideoPlayerConfig.GlassyUIConfig()
) {
    val chipConfig = if (isSelected) {
        config.copy(
            backgroundOpacity = 0.3f,
            borderOpacity = 0.5f
        )
    } else {
        config
    }

    GlassyContainer(
        modifier = Modifier.clickable { onClick() },
        config = chipConfig,
        shape = RoundedCornerShape(20.dp),
        contentPadding = 0.dp
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${speed}x",
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}