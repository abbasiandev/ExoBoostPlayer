package dev.abbasian.exoboostplayer.presentation.ui.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.abbasian.exoboostplayer.presentation.Demo
import dev.abbasian.exoboostplayer.presentation.DemoCategory
import dev.abbasian.exoboostplayer.presentation.DemoDifficulty
import dev.abbasian.exoboostplayer.presentation.DemoItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoList(onDemoSelected: (Demo) -> Unit) {
    val categories = remember {
        listOf(
            DemoCategory(
                title = "AI Features",
                description = "Intelligent media processing with TensorFlow Lite",
                icon = Icons.Filled.AutoAwesome,
                demos = listOf(
                    DemoItem(
                        title = "AI Thumbnail Generation",
                        description = "Smart frame selection using machine learning",
                        features = listOf(
                            "Quality scoring",
                            "Scene detection",
                            "ML-powered selection",
                            "Best frame picking"
                        ),
                        difficulty = DemoDifficulty.INTERMEDIATE,
                        demo = Demo.AIThumbnails("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
                    ),
                    DemoItem(
                        title = "Video Content Analysis",
                        description = "Automatic scene detection and key moments",
                        features = listOf(
                            "Scene boundaries",
                            "Key moments",
                            "Color analysis",
                            "Content summary"
                        ),
                        difficulty = DemoDifficulty.ADVANCED,
                        demo = Demo.VideoAnalysis("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4")
                    )
                )
            ),
            DemoCategory(
                title = "Video Playback",
                description = "Core video player capabilities",
                icon = Icons.Filled.VideoLibrary,
                demos = listOf(
                    DemoItem(
                        title = "Basic Video Player",
                        description = "Simple video playback with minimal configuration",
                        features = listOf("Auto-play", "Basic controls", "Buffering"),
                        difficulty = DemoDifficulty.BEGINNER,
                        demo = Demo.VideoBasic("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
                    ),
                    DemoItem(
                        title = "Advanced Player",
                        description = "Full-featured player with gestures & quality control",
                        features = listOf(
                            "Swipe gestures",
                            "Quality selection",
                            "Speed control",
                            "Glassy UI"
                        ),
                        difficulty = DemoDifficulty.INTERMEDIATE,
                        demo = Demo.VideoAdvanced("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4")
                    ),
                )
            ),
            DemoCategory(
                title = "Error Recovery",
                description = "Resilience and failure handling",
                icon = Icons.Filled.Healing,
                demos = listOf(
                    DemoItem(
                        title = "Network Recovery",
                        description = "Auto-retry with exponential backoff",
                        features = listOf(
                            "5 retry attempts",
                            "Exponential backoff",
                            "Network state monitoring",
                            "Visual retry feedback"
                        ),
                        difficulty = DemoDifficulty.INTERMEDIATE,
                        demo = Demo.VideoErrorRecovery("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8")
                    ),
                    DemoItem(
                        title = "Error Comparison",
                        description = "ExoBoost vs Vanilla ExoPlayer error handling",
                        features = listOf(
                            "Side-by-side comparison",
                            "Error injection",
                            "Recovery metrics",
                            "Success rate tracking"
                        ),
                        difficulty = DemoDifficulty.ADVANCED,
                        demo = Demo.ErrorComparison("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8")
                    )
                )
            ),
            DemoCategory(
                title = "Audio Excellence",
                description = "Audio playback with visualization",
                icon = Icons.Filled.AudioFile,
                demos = listOf(
                    DemoItem(
                        title = "Audio Visualization",
                        description = "5 visualization types with real-time audio analysis",
                        features = listOf(
                            "Spectrum analyzer",
                            "Waveform",
                            "Circular",
                            "Bars",
                            "Particle system"
                        ),
                        difficulty = DemoDifficulty.INTERMEDIATE,
                        demo = Demo.AudioVisualization(
                            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                            "Soundhelix Song 1",
                            "Soundhelix"
                        )
                    ),
                    DemoItem(
                        title = "8-Band Equalizer",
                        description = "Professional audio equalizer with presets",
                        features = listOf(
                            "8 frequency bands",
                            "Custom presets",
                            "Real-time adjustment",
                            "Bass/Treble boost"
                        ),
                        difficulty = DemoDifficulty.INTERMEDIATE,
                        demo = Demo.AudioEqualizer(
                            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                            "Soundhelix Song 2",
                            "Soundhelix"
                        )
                    ),
                    DemoItem(
                        title = "Playlist Management",
                        description = "Full playlist with navigation",
                        features = listOf(
                            "Next/Previous",
                            "Track info",
                            "Seamless transitions",
                            "State preservation"
                        ),
                        difficulty = DemoDifficulty.BEGINNER,
                        demo = Demo.AudioPlaylist
                    )
                )
            ),
            DemoCategory(
                title = "Performance & Quality",
                description = "Optimization and quality management",
                icon = Icons.Filled.Speed,
                demos = listOf(
                    DemoItem(
                        title = "Quality Selector",
                        description = "Manual quality selection with preview",
                        features = listOf(
                            "Auto/Manual modes",
                            "Available qualities",
                            "Bitrate display",
                            "Smooth transitions"
                        ),
                        difficulty = DemoDifficulty.BEGINNER,
                        demo = Demo.VideoQualityControl("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4")
                    ),
                    DemoItem(
                        title = "Buffer Optimization",
                        description = "Custom buffer strategies visualization",
                        features = listOf(
                            "Buffer health display",
                            "Rebuffer tracking",
                            "Custom configurations",
                            "Performance metrics"
                        ),
                        difficulty = DemoDifficulty.ADVANCED,
                        demo = Demo.BufferVisualization("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8")
                    ),
                    DemoItem(
                        title = "Cache Management",
                        description = "Video caching with size monitoring",
                        features = listOf(
                            "Cache size tracking",
                            "Cache hit rate",
                            "Clear cache",
                            "Offline playback"
                        ),
                        difficulty = DemoDifficulty.INTERMEDIATE,
                        demo = Demo.CacheDemo("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4")
                    )
                )
            ),
            DemoCategory(
                title = "Real-World Scenarios",
                description = "Production-ready use cases",
                icon = Icons.Filled.RocketLaunch,
                demos = listOf(
                    DemoItem(
                        title = "Poor Network Simulation",
                        description = "Test player behavior under bad conditions",
                        features = listOf(
                            "Simulated network throttling",
                            "Packet loss",
                            "Latency injection",
                            "Recovery visualization"
                        ),
                        difficulty = DemoDifficulty.ADVANCED,
                        demo = Demo.NetworkSimulation("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
                    ),
                )
            ),
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A0A0A), Color.Black)
                )
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Text(
                    text = "ExoBoost Showcase",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Explore powerful features that solve real Media3 challenges",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        categories.forEach { category ->
            item {
                CategorySection(category, onDemoSelected)
            }
        }
    }
}

@Composable
private fun CategorySection(
    category: DemoCategory,
    onDemoSelected: (Demo) -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        category.demos.forEach { demo ->
            DemoCard(demo, onDemoSelected)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DemoCard(
    demoItem: DemoItem,
    onDemoSelected: (Demo) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDemoSelected(demoItem.demo) },
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = demoItem.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                DifficultyBadge(demoItem.difficulty)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = demoItem.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                demoItem.features.forEach { feature ->
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DifficultyBadge(difficulty: DemoDifficulty) {
    val (color, text) = when (difficulty) {
        DemoDifficulty.BEGINNER -> Color(0xFF4CAF50) to "Beginner"
        DemoDifficulty.INTERMEDIATE -> Color(0xFFFF9800) to "Intermediate"
        DemoDifficulty.ADVANCED -> Color(0xFFF44336) to "Advanced"
    }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }

        var xPosition = 0
        var yPosition = 0
        var maxHeight = 0
        val rows = mutableListOf<List<Pair<Placeable, IntOffset>>>()
        var currentRow = mutableListOf<Pair<Placeable, IntOffset>>()

        placeables.forEach { placeable ->
            if (xPosition + placeable.width > constraints.maxWidth) {
                rows.add(currentRow)
                currentRow = mutableListOf()
                xPosition = 0
                yPosition += maxHeight + 8.dp.roundToPx()
                maxHeight = 0
            }

            currentRow.add(placeable to IntOffset(xPosition, yPosition))
            xPosition += placeable.width + 8.dp.roundToPx()
            maxHeight = maxOf(maxHeight, placeable.height)
        }

        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        val totalHeight = yPosition + maxHeight

        layout(constraints.maxWidth, totalHeight) {
            rows.flatten().forEach { (placeable, offset) ->
                placeable.place(offset)
            }
        }
    }
}