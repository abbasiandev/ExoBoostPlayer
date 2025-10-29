package dev.abbasian.exoboostplayer.presentation.ui.demo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.abbasian.exoboost.domain.model.MediaPlayerConfig
import dev.abbasian.exoboost.presentation.ui.screen.exoBoostAudioPlayer
import dev.abbasian.exoboostplayer.presentation.Track

@Composable
fun PlaylistDemo(onBack: () -> Unit) {
    val playlist =
        remember {
            listOf(
                Track(
                    "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                    "Song 1",
                    "SoundHelix",
                ),
                Track(
                    "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                    "Song 2",
                    "SoundHelix",
                ),
                Track(
                    "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                    "Song 3",
                    "SoundHelix",
                ),
            )
        }
    var currentIndex by remember { mutableIntStateOf(0) }
    val currentTrack = playlist[currentIndex]

    exoBoostAudioPlayer(
        audioUrl = currentTrack.url,
        trackTitle = currentTrack.title,
        artistName = currentTrack.artist,
        currentTrackIndex = currentIndex,
        totalTracks = playlist.size,
        mediaConfig =
            MediaPlayerConfig(
                autoPlay = true,
                audioVisualization = MediaPlayerConfig.AudioVisualizationConfig(enableVisualization = true),
            ),
        onNext = { if (currentIndex < playlist.size - 1) currentIndex++ },
        onPrevious = { if (currentIndex > 0) currentIndex-- },
        onBack = onBack,
        modifier = Modifier.fillMaxSize(),
    )
}
