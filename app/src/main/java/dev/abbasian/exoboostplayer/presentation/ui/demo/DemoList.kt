package dev.abbasian.exoboostplayer.presentation.ui.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.abbasian.exoboostplayer.presentation.Demo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoList(onDemoSelected: (Demo) -> Unit) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ExoBoost Power Features", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "VIDEO FEATURES",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            DemoCard(
                title = "Basic Video Playback",
                description = "Standard video with all controls",
                onClick = { onDemoSelected(Demo.VideoBasic("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")) }
            )

            DemoCard(
                title = "Advanced Features",
                description = "Speed control, quality selection, gestures, fullscreen",
                onClick = { onDemoSelected(Demo.VideoAdvanced("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8")) }
            )

            DemoCard(
                title = "Automatic Error Recovery",
                description = "Auto-retry, quality downgrade, decoder fallback",
                onClick = { onDemoSelected(Demo.VideoErrorRecovery("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8")) }
            )

            DemoCard(
                title = "Manual Quality Control",
                description = "Switch between 360p, 480p, 720p, 1080p, Auto",
                onClick = { onDemoSelected(Demo.VideoQualityControl("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8")) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "AUDIO FEATURES",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            DemoCard(
                title = "Audio Visualization",
                description = "5 types: Spectrum, Waveform, Circular, Bars, Particles",
                onClick = {
                    onDemoSelected(
                        Demo.AudioVisualization(
                            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                            "SoundHelix Song 1",
                            "SoundHelix"
                        )
                    )
                }
            )

            DemoCard(
                title = "8-Band Equalizer",
                description = "Real-time audio equalizer with frequency control",
                onClick = {
                    onDemoSelected(
                        Demo.AudioEqualizer(
                            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                            "SoundHelix Song 2",
                            "SoundHelix"
                        )
                    )
                }
            )

            DemoCard(
                title = "Playlist with Navigation",
                description = "Next/Previous track navigation",
                onClick = { onDemoSelected(Demo.AudioPlaylist) }
            )
        }
    }
}

@Composable
private fun DemoCard(title: String, description: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}