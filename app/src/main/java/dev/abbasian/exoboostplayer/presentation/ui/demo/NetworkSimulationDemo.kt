package dev.abbasian.exoboostplayer.presentation.ui.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularAlt1Bar
import androidx.compose.material.icons.filled.SignalCellularAlt2Bar
import androidx.compose.material.icons.filled.SignalCellularConnectedNoInternet0Bar
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.abbasian.exoboost.domain.model.MediaPlayerConfig
import dev.abbasian.exoboost.presentation.ui.screen.ExoBoostPlayer
import dev.abbasian.exoboost.presentation.viewmodel.MediaPlayerViewModel
import dev.abbasian.exoboostplayer.presentation.NetworkSimulation
import org.koin.androidx.compose.koinViewModel

@Composable
fun NetworkSimulationDemo(url: String, onBack: () -> Unit) {
    val viewModel: MediaPlayerViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()

    var currentSimulation by remember { mutableStateOf(NetworkSimulation.PERFECT) }
    var showSimulator by remember { mutableStateOf(true) }
    var simulationEvents by remember { mutableStateOf<List<String>>(emptyList()) }

    val config = remember(currentSimulation) {
        when (currentSimulation) {
            NetworkSimulation.PERFECT -> MediaPlayerConfig(
                autoPlay = true,
                showControls = true,
                bufferDurations = MediaPlayerConfig.BufferDurations(
                    minBufferMs = 30000,
                    maxBufferMs = 60000,
                    bufferForPlaybackMs = 2500,
                    bufferForPlaybackAfterRebufferMs = 5000
                )
            )
            NetworkSimulation.GOOD -> MediaPlayerConfig(
                autoPlay = true,
                showControls = true,
                bufferDurations = MediaPlayerConfig.BufferDurations(
                    minBufferMs = 20000,
                    maxBufferMs = 50000,
                    bufferForPlaybackMs = 3000,
                    bufferForPlaybackAfterRebufferMs = 6000
                )
            )
            NetworkSimulation.MODERATE -> MediaPlayerConfig(
                autoPlay = true,
                showControls = true,
                retryOnError = true,
                maxRetryCount = 3,
                bufferDurations = MediaPlayerConfig.BufferDurations(
                    minBufferMs = 15000,
                    maxBufferMs = 40000,
                    bufferForPlaybackMs = 4000,
                    bufferForPlaybackAfterRebufferMs = 8000
                )
            )
            NetworkSimulation.POOR -> MediaPlayerConfig(
                autoPlay = true,
                showControls = true,
                retryOnError = true,
                maxRetryCount = 5,
                autoQualityOnError = true,
                bufferDurations = MediaPlayerConfig.BufferDurations(
                    minBufferMs = 10000,
                    maxBufferMs = 30000,
                    bufferForPlaybackMs = 5000,
                    bufferForPlaybackAfterRebufferMs = 10000
                )
            )
            NetworkSimulation.VERY_POOR -> MediaPlayerConfig(
                autoPlay = true,
                showControls = true,
                retryOnError = true,
                maxRetryCount = 10,
                autoQualityOnError = true,
                preferSoftwareDecoder = true,
                bufferDurations = MediaPlayerConfig.BufferDurations(
                    minBufferMs = 5000,
                    maxBufferMs = 20000,
                    bufferForPlaybackMs = 8000,
                    bufferForPlaybackAfterRebufferMs = 15000
                )
            )
            NetworkSimulation.OFFLINE -> MediaPlayerConfig(
                autoPlay = false,
                showControls = true,
                retryOnError = true,
                maxRetryCount = 5
            )
        }
    }

    LaunchedEffect(currentSimulation) {
        simulationEvents = simulationEvents + "[${System.currentTimeMillis()}] Switched to ${currentSimulation.name}"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ExoBoostPlayer(
            videoUrl = url,
            mediaConfig = config,
            onBack = onBack,
            modifier = Modifier.fillMaxSize()
        )

        if (showSimulator) {
            NetworkSimulatorPanel(
                currentSimulation = currentSimulation,
                simulationEvents = simulationEvents.takeLast(10),
                onSimulationChange = {
                    currentSimulation = it
                    viewModel.resetPlayer()

                    viewModel.loadMedia(url, config)
                },
                onClearEvents = { simulationEvents = emptyList() },
                onDismiss = { showSimulator = false }
            )
        }

        FloatingActionButton(
            onClick = { showSimulator = !showSimulator },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                if (showSimulator) Icons.Filled.VisibilityOff else Icons.Filled.NetworkCheck,
                contentDescription = "Toggle simulator"
            )
        }
    }
}

@Composable
private fun NetworkSimulatorPanel(
    currentSimulation: NetworkSimulation,
    simulationEvents: List<String>,
    onSimulationChange: (NetworkSimulation) -> Unit,
    onClearEvents: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Network Simulator",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current simulation indicator
            Surface(
                color = getSimulationColor(currentSimulation).copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = getSimulationIcon(currentSimulation),
                        contentDescription = null,
                        tint = getSimulationColor(currentSimulation),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Current: ${currentSimulation.name.replace("_", " ")}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = getSimulationDescription(currentSimulation),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Simulation options
            Text(
                text = "Select Network Condition",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NetworkSimulation.values().forEach { simulation ->
                    SimulationButton(
                        simulation = simulation,
                        isSelected = simulation == currentSimulation,
                        onClick = { onSimulationChange(simulation) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Event log
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Events (${simulationEvents.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.7f)
                )
                if (simulationEvents.isNotEmpty()) {
                    TextButton(onClick = onClearEvents) {
                        Text("Clear", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (simulationEvents.isEmpty()) {
                Text(
                    text = "No events yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color.White.copy(alpha = 0.05f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    simulationEvents.reversed().forEach { event ->
                        Text(
                            text = event,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info card
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "How It Works",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "ExoBoost adapts buffer sizes, retry strategies, and quality settings based on network conditions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SimulationButton(
    simulation: NetworkSimulation,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) {
                getSimulationColor(simulation)
            } else {
                Color.White.copy(alpha = 0.1f)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = getSimulationIcon(simulation),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = simulation.name.replace("_", " "),
                    color = Color.White,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun getSimulationColor(simulation: NetworkSimulation): Color {
    return when (simulation) {
        NetworkSimulation.PERFECT -> Color(0xFF4CAF50)
        NetworkSimulation.GOOD -> Color(0xFF8BC34A)
        NetworkSimulation.MODERATE -> Color(0xFFFFEB3B)
        NetworkSimulation.POOR -> Color(0xFFFF9800)
        NetworkSimulation.VERY_POOR -> Color(0xFFF44336)
        NetworkSimulation.OFFLINE -> Color(0xFF9E9E9E)
    }
}

private fun getSimulationIcon(simulation: NetworkSimulation): androidx.compose.ui.graphics.vector.ImageVector {
    return when (simulation) {
        NetworkSimulation.PERFECT -> Icons.Filled.SignalCellularAlt
        NetworkSimulation.GOOD -> Icons.Filled.SignalCellular4Bar
        NetworkSimulation.MODERATE -> Icons.Filled.SignalCellularAlt
        NetworkSimulation.POOR -> Icons.Filled.SignalCellularAlt2Bar
        NetworkSimulation.VERY_POOR -> Icons.Filled.SignalCellularAlt1Bar
        NetworkSimulation.OFFLINE -> Icons.Filled.SignalCellularConnectedNoInternet0Bar
    }
}

private fun getSimulationDescription(simulation: NetworkSimulation): String {
    return when (simulation) {
        NetworkSimulation.PERFECT -> "Optimal conditions, minimal buffering"
        NetworkSimulation.GOOD -> "Stable connection, smooth playback"
        NetworkSimulation.MODERATE -> "Occasional delays, auto-retry enabled"
        NetworkSimulation.POOR -> "Frequent buffering, quality downgrade active"
        NetworkSimulation.VERY_POOR -> "Severe instability, aggressive recovery"
        NetworkSimulation.OFFLINE -> "No connection, testing offline behavior"
    }
}