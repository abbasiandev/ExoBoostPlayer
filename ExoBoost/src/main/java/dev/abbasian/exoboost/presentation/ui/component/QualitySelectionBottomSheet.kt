package dev.abbasian.exoboost.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.abbasian.exoboost.R
import dev.abbasian.exoboost.domain.model.VideoQuality

@Composable
fun QualitySelectionBottomSheet(
    availableQualities: List<VideoQuality>,
    currentQuality: VideoQuality?,
    onQualitySelected: (VideoQuality) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val glassColor = if (isDarkTheme) Color.White else Color.Black

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
                imageVector = Icons.Default.HighQuality,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = context.getString(R.string.video_quality),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // quality options
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(availableQualities) { quality ->
                QualityItem(
                    quality = quality,
                    isSelected = quality == currentQuality,
                    onClick = {
                        onQualitySelected(quality)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun QualityItem(
    quality: VideoQuality,
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = 0.5.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                } else {
                    glassColor.copy(alpha = 0.15f)
                },
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = quality.getQualityLabel(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            Icon(
                imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}