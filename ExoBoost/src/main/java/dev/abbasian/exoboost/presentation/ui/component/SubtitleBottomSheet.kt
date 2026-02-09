package dev.abbasian.exoboost.presentation.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.abbasian.exoboost.domain.model.SubtitleStyle
import dev.abbasian.exoboost.domain.model.SubtitleTrack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun subtitleBottomSheet(
    availableSubtitles: List<SubtitleTrack>,
    currentSubtitle: SubtitleTrack?,
    currentStyle: SubtitleStyle,
    onSubtitleSelected: (SubtitleTrack?) -> Unit,
    onStyleChanged: (SubtitleStyle) -> Unit,
    onSearchSubtitles: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onLoadFromFile: (() -> Unit)? = null,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Black.copy(alpha = 0.95f),
        contentColor = Color.White,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Subtitle Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                    )
                }
            }

            Divider(color = Color.White.copy(alpha = 0.2f))

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Language",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            SubtitleLanguageCard(
                title = "No Subtitles",
                subtitle = "Disable subtitles",
                isSelected = currentSubtitle == null,
                onClick = { onSubtitleSelected(null) },
            )

            Spacer(modifier = Modifier.height(8.dp))

            availableSubtitles.forEach { subtitle ->
                SubtitleLanguageCard(
                    title = subtitle.language,
                    subtitle = "${subtitle.source.name} • ${subtitle.format.name}",
                    isSelected = currentSubtitle?.id == subtitle.id,
                    onClick = { onSubtitleSelected(subtitle) },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onSearchSubtitles,
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        ),
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Search", style = MaterialTheme.typography.bodyMedium)
                }

                if (onLoadFromFile != null) {
                    Button(
                        onClick = onLoadFromFile,
                        modifier =
                            Modifier
                                .weight(1f)
                                .padding(vertical = 8.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                            ),
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Load File", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider(color = Color.White.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Appearance",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            subtitleSizeSelector(
                currentSize = currentStyle.textSize,
                onSizeChanged = { newSize ->
                    onStyleChanged(currentStyle.copy(textSize = newSize))
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            subtitleColorPicker(
                label = "Text Color",
                currentColor = Color(currentStyle.textColor),
                onColorSelected = { color ->
                    onStyleChanged(currentStyle.copy(textColor = color.hashCode()))
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            subtitleColorPicker(
                label = "Background Color",
                currentColor = Color(currentStyle.backgroundColor),
                onColorSelected = { color ->
                    onStyleChanged(currentStyle.copy(backgroundColor = color.hashCode()))
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            subtitleBackgroundOpacitySlider(
                currentOpacity = currentStyle.backgroundOpacity,
                onOpacityChanged = { opacity ->
                    onStyleChanged(currentStyle.copy(backgroundOpacity = opacity))
                },
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SubtitleLanguageCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    } else {
                        Color.White.copy(alpha = 0.1f)
                    },
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                )
            }

            if (isSelected) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun compactSubtitleSheet(
    availableSubtitles: List<SubtitleTrack>,
    currentSubtitle: SubtitleTrack?,
    onSubtitleSelected: (SubtitleTrack?) -> Unit,
    onOpenFullSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Black.copy(alpha = 0.95f),
        contentColor = Color.White,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Text(
                "Select Subtitle",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            ListItem(
                headlineContent = { Text("No Subtitles", color = Color.White) },
                leadingContent = {
                    RadioButton(
                        selected = currentSubtitle == null,
                        onClick = null,
                    )
                },
                modifier =
                    Modifier.clickable {
                        onSubtitleSelected(null)
                        onDismiss()
                    },
            )

            availableSubtitles.forEach { subtitle ->
                ListItem(
                    headlineContent = { Text(subtitle.language, color = Color.White) },
                    supportingContent = {
                        Text(
                            subtitle.source.name,
                            color = Color.White.copy(alpha = 0.6f),
                        )
                    },
                    leadingContent = {
                        RadioButton(
                            selected = currentSubtitle?.id == subtitle.id,
                            onClick = null,
                        )
                    },
                    modifier =
                        Modifier.clickable {
                            onSubtitleSelected(subtitle)
                            onDismiss()
                        },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onOpenFullSettings,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Advanced Settings")
            }
        }
    }
}
