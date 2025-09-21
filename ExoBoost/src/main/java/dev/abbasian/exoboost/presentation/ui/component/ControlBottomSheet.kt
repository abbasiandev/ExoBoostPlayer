package dev.abbasian.exoboost.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.abbasian.exoboost.R

@Composable
fun SpeedControlBottomSheet(
    currentSpeed: Float,
    availableSpeeds: List<Float>,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()

    val glassColor = if (isDarkTheme) Color.White else Color.Black
    val textColor = if (isDarkTheme) Color.White else Color.Black

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glassColor.copy(alpha = 0.1f),
                        glassColor.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .border(
                width = 0.5.dp,
                color = glassColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .padding(16.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
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
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // current speed indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
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

@Composable
private fun SpeedChip(
    speed: Float,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val glassColor = if (isDarkTheme) Color.White else Color.Black

    val backgroundColor = if (isSelected) {
        Brush.radialGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )
        )
    } else {
        Brush.radialGradient(
            colors = listOf(
                glassColor.copy(alpha = 0.1f),
                glassColor.copy(alpha = 0.05f)
            )
        )
    }

    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(
                width = 0.5.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                } else {
                    glassColor.copy(alpha = 0.15f)
                },
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${speed}x",
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}