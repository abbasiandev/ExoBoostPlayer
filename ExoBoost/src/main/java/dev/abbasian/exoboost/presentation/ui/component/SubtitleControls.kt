package dev.abbasian.exoboost.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.abbasian.exoboost.domain.model.SubtitleSize
import dev.abbasian.exoboost.domain.model.SubtitleTrack

@Composable
fun subtitleLanguageSelector(
    availableSubtitles: List<SubtitleTrack>,
    currentSubtitle: SubtitleTrack?,
    onSubtitleSelected: (SubtitleTrack?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        glassyIconButton(
            icon = Icons.Default.Subtitles,
            onClick = { expanded = true },
            contentDescription = "Subtitle settings",
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier =
                Modifier
                    .background(Color.Black.copy(alpha = 0.9f))
                    .widthIn(min = 200.dp),
        ) {
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (currentSubtitle == null) {
                            Icon(
                                Icons.Default.Subtitles,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Text(
                            "No Subtitles",
                            color =
                                if (currentSubtitle == null) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.White
                                },
                        )
                    }
                },
                onClick = {
                    onSubtitleSelected(null)
                    expanded = false
                },
            )

            Divider(color = Color.White.copy(alpha = 0.2f))

            availableSubtitles.forEach { subtitle ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (currentSubtitle?.id == subtitle.id) {
                                Icon(
                                    Icons.Default.Subtitles,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Column {
                                Text(
                                    subtitle.language,
                                    color =
                                        if (currentSubtitle?.id == subtitle.id) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            Color.White
                                        },
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    subtitle.source.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f),
                                )
                            }
                        }
                    },
                    onClick = {
                        onSubtitleSelected(subtitle)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun subtitleSizeSelector(
    currentSize: SubtitleSize,
    onSizeChanged: (SubtitleSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp),
        ) {
            Icon(
                Icons.Default.TextFields,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Subtitle Size",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SubtitleSize.values().forEach { size ->
                SubtitleSizeButton(
                    size = size,
                    isSelected = currentSize == size,
                    onClick = { onSizeChanged(size) },
                )
            }
        }

        Slider(
            value = currentSize.ordinal.toFloat(),
            onValueChange = { value ->
                onSizeChanged(SubtitleSize.values()[value.toInt()])
            },
            valueRange = 0f..(SubtitleSize.values().size - 1).toFloat(),
            steps = SubtitleSize.values().size - 2,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            colors =
                SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                ),
        )
    }
}

@Composable
private fun SubtitleSizeButton(
    size: SubtitleSize,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val label =
        when (size) {
            SubtitleSize.VERY_SMALL -> "Very\nSmall"
            SubtitleSize.SMALL -> "Small"
            SubtitleSize.MEDIUM -> "Medium"
            SubtitleSize.LARGE -> "Large"
            SubtitleSize.VERY_LARGE -> "Very\nLarge"
        }

    Box(
        modifier =
            Modifier
                .width(60.dp)
                .height(50.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.White.copy(alpha = 0.2f)
                    },
                ).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) Color.Black else Color.White,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
fun subtitleColorPicker(
    label: String,
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp),
        ) {
            Icon(
                Icons.Default.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }

        val colors =
            listOf(
                Color.White,
                Color.Yellow,
                Color.Cyan,
                Color.Green,
                Color.Red,
                Color.Magenta,
                Color.Blue,
                Color.Black,
            )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            colors.forEach { color ->
                ColorButton(
                    color = color,
                    isSelected = currentColor == color,
                    onClick = { onColorSelected(color) },
                )
            }
        }
    }
}

@Composable
private fun ColorButton(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(color)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.3f)),
            )
        }
    }
}

@Composable
fun subtitleBackgroundOpacitySlider(
    currentOpacity: Float,
    onOpacityChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
    ) {
        Text(
            "Background Opacity",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Transparent",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
            )

            Slider(
                value = currentOpacity,
                onValueChange = onOpacityChanged,
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f),
                colors =
                    SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                    ),
            )

            Text(
                "Opaque",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
            )
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = currentOpacity)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Preview",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
