package dev.abbasian.exoboostplayer.presentation.ui.demo

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.abbasian.exoboost.domain.model.HighlightConfig
import dev.abbasian.exoboost.domain.model.MediaPlayerConfig
import dev.abbasian.exoboost.domain.model.VisualizationColorScheme
import dev.abbasian.exoboost.domain.model.VisualizationType
import dev.abbasian.exoboost.presentation.ui.screen.exoBoostAudioPlayer
import dev.abbasian.exoboost.presentation.viewmodel.MediaPlayerViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun AdvancedAudioVisualizationDemo(
    url: String,
    title: String,
    artist: String,
    onBack: () -> Unit,
) {
    val viewModel: MediaPlayerViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()

    var visualizationType by remember { mutableStateOf(VisualizationType.SPECTRUM) }
    var colorScheme by remember { mutableStateOf(VisualizationColorScheme.DYNAMIC) }
    var sensitivity by remember { mutableStateOf(1.5f) }
    var smoothingFactor by remember { mutableStateOf(0.85f) }
    var showSettings by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        exoBoostAudioPlayer(
            audioUrl = url,
            trackTitle = title,
            artistName = artist,
            mediaConfig =
                MediaPlayerConfig(
                    autoPlay = true,
                    audioVisualization =
                        MediaPlayerConfig.AudioVisualizationConfig(
                            enableVisualization = true,
                            colorScheme = colorScheme,
                            sensitivity = sensitivity,
                            smoothingFactor = smoothingFactor,
                        ),
                    glassyUI =
                        MediaPlayerConfig.GlassyUIConfig(
                            blurRadius = 35f,
                            borderOpacity = 0.45f,
                        ),
                    highlightConfig = HighlightConfig.audioFocused(),
                ),
            currentVisualizationType = visualizationType,
            onVisualizationTypeChange = { visualizationType = it },
            onBack = onBack,
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedContent(
            targetState = showSettings,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "settings",
        ) { isVisible ->
            if (isVisible) {
                VisualizationSettingsPanel(
                    colorScheme = colorScheme,
                    sensitivity = sensitivity,
                    smoothingFactor = smoothingFactor,
                    onColorSchemeChange = { colorScheme = it },
                    onSensitivityChange = { sensitivity = it },
                    onSmoothingChange = { smoothingFactor = it },
                    onDismiss = { showSettings = false },
                )
            }
        }

        FloatingActionButton(
            onClick = { showSettings = !showSettings },
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
        ) {
            Icon(
                if (showSettings) Icons.Filled.Close else Icons.Filled.Tune,
                contentDescription = "Visualization settings",
            )
        }
    }
}

@Composable
private fun VisualizationSettingsPanel(
    colorScheme: VisualizationColorScheme,
    sensitivity: Float,
    smoothingFactor: Float,
    onColorSchemeChange: (VisualizationColorScheme) -> Unit,
    onSensitivityChange: (Float) -> Unit,
    onSmoothingChange: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.95f),
            ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Visualization Settings",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Color Scheme",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.7f),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ColorSchemeButton(
                    scheme = VisualizationColorScheme.DYNAMIC,
                    currentScheme = colorScheme,
                    onClick = { onColorSchemeChange(VisualizationColorScheme.DYNAMIC) },
                )
                ColorSchemeButton(
                    scheme = VisualizationColorScheme.MONOCHROME,
                    currentScheme = colorScheme,
                    onClick = { onColorSchemeChange(VisualizationColorScheme.MONOCHROME) },
                )
                ColorSchemeButton(
                    scheme = VisualizationColorScheme.RAINBOW,
                    currentScheme = colorScheme,
                    onClick = { onColorSchemeChange(VisualizationColorScheme.RAINBOW) },
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Sensitivity: ${String.format("%.1f", sensitivity)}x",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.7f),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = sensitivity,
                onValueChange = onSensitivityChange,
                valueRange = 0.5f..3.0f,
                colors =
                    SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                    ),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Smoothing: ${(smoothingFactor * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.7f),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = smoothingFactor,
                onValueChange = onSmoothingChange,
                valueRange = 0.5f..0.95f,
                colors =
                    SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                    ),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        Icons.Filled.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Pro Tip",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "Increase sensitivity for quiet tracks. Higher smoothing reduces jitter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSchemeButton(
    scheme: VisualizationColorScheme,
    currentScheme: VisualizationColorScheme,
    onClick: () -> Unit,
) {
    val isSelected = scheme == currentScheme
    val gradientColors =
        when (scheme) {
            VisualizationColorScheme.DYNAMIC -> listOf(Color(0xFF2196F3), Color(0xFF9C27B0))
            VisualizationColorScheme.MONOCHROME -> listOf(Color.White, Color.Gray)
            VisualizationColorScheme.RAINBOW ->
                listOf(
                    Color(0xFFFF0000),
                    Color(0xFFFFFF00),
                    Color(0xFF00FF00),
                    Color(0xFF00FFFF),
                    Color(0xFF0000FF),
                )

            VisualizationColorScheme.MATERIAL_YOU -> listOf(Color(0xFFFFFF00), Color(0xFFFF0000))
        }

    Box(
        modifier =
            Modifier
                .height(48.dp)
                .background(
                    brush = Brush.horizontalGradient(gradientColors),
                    shape = RoundedCornerShape(12.dp),
                ).then(
                    if (isSelected) {
                        Modifier.border(
                            2.dp,
                            Color.White,
                            RoundedCornerShape(12.dp),
                        )
                    } else {
                        Modifier
                    },
                ),
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                ),
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(
                text = scheme.name.lowercase().capitalize(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

private fun String.capitalize(): String = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
