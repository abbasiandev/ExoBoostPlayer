# ExoBoost

Enhanced ExoPlayer wrapper with intelligent error handling, automatic recovery, and adaptive quality switching for robust media playback.

[![Maven Central](https://img.shields.io/maven-central/v/dev.abbasian/exoboost?color=blue)](https://search.maven.org/search?q=g:dev.abbasian%20AND%20a:exoboost)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)
[![API Level](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://developer.android.com/guide/topics/manifest/uses-sdk-element#ApiLevels)

---

## Features

- **Ready-to-use Compose Components**: Drop-in video and audio players
- **Intelligent Error Recovery**: Automatic retry with exponential backoff
- **Adaptive Quality Management**: Prevents playback failures during network issues
- **Hardware/Software Codec Fallback**: Seamless decoder switching
- **Advanced Audio Features**: Built-in equalizer and audio visualization
- **Glassy UI Design**: Modern blur effects and smooth animations
- **Gesture Controls**: Touch gestures for seek, volume, and brightness
- **Speed Control**: Multiple playback speed options
- **Quality Selection**: Manual and automatic quality switching

---

## Installation

Add ExoBoost to your `build.gradle` (Module: app):

```gradle
dependencies {
    implementation "dev.abbasian:exoboost:1.0.0"
}
```

Initialize ExoBoost in your Application class:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ExoBoost.initialize(this)
    }
}
```

---

## Quick Start

### Basic Video Player

```kotlin
import dev.abbasian.exoboost.domain.model.MediaPlayerConfig
import dev.abbasian.exoboost.presentation.ui.screen.ExoBoostPlayer

@Composable
fun BasicVideoPlayer() {
    ExoBoostPlayer(
        videoUrl = "https://example.com/video.mp4",
        mediaConfig = MediaPlayerConfig(
            autoPlay = true,
            showControls = true
        ),
        onBack = { /* Handle back navigation */ },
        modifier = Modifier.fillMaxSize()
    )
}
```

### Advanced Video Player with All Features

```kotlin
@Composable
fun AdvancedVideoPlayer() {
    ExoBoostPlayer(
        videoUrl = "https://example.com/video.mp4",
        mediaConfig = MediaPlayerConfig(
            autoPlay = true,
            showControls = true,
            enableGestures = true,
            enableSpeedControl = true,
            enableQualitySelection = true,
            playbackSpeedOptions = listOf(
                0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f
            ),
            glassyUI = MediaPlayerConfig.GlassyUIConfig(
                blurRadius = 30f,
                borderOpacity = 0.4f
            )
        ),
        onBack = { /* Handle back navigation */ },
        modifier = Modifier.fillMaxSize()
    )
}
```

### Audio Player with Equalizer

```kotlin
import dev.abbasian.exoboost.domain.model.VisualizationColorScheme
import dev.abbasian.exoboost.presentation.ui.screen.ExoBoostAudioPlayer

@Composable
fun AudioPlayerWithEqualizer() {
    ExoBoostAudioPlayer(
        audioUrl = "https://example.com/audio.mp3",
        trackTitle = "Song Title",
        artistName = "Artist Name",
        mediaConfig = MediaPlayerConfig(
            autoPlay = true,
            audioVisualization = MediaPlayerConfig.AudioVisualizationConfig(
                enableVisualization = true,
                colorScheme = VisualizationColorScheme.DYNAMIC
            )
        ),
        onBack = { /* Handle back navigation */ },
        modifier = Modifier.fillMaxSize()
    )
}
```

### Audio Playlist Example

```kotlin
@Composable
fun PlaylistPlayer() {
    val playlist = remember {
        listOf(
            Track("https://example.com/song1.mp3", "Song 1", "Artist 1"),
            Track("https://example.com/song2.mp3", "Song 2", "Artist 2"),
            Track("https://example.com/song3.mp3", "Song 3", "Artist 3")
        )
    }
    
    var currentIndex by remember { mutableIntStateOf(0) }
    val currentTrack = playlist[currentIndex]
    
    ExoBoostAudioPlayer(
        audioUrl = currentTrack.url,
        trackTitle = currentTrack.title,
        artistName = currentTrack.artist,
        currentTrackIndex = currentIndex,
        totalTracks = playlist.size,
        mediaConfig = MediaPlayerConfig(
            autoPlay = true,
            audioVisualization = MediaPlayerConfig.AudioVisualizationConfig(
                enableVisualization = true
            )
        ),
        onNext = { if (currentIndex < playlist.size - 1) currentIndex++ },
        onPrevious = { if (currentIndex > 0) currentIndex-- },
        onBack = { /* Handle back navigation */ },
        modifier = Modifier.fillMaxSize()
    )
}

data class Track(val url: String, val title: String, val artist: String)
```

---

## Configuration Options

### MediaPlayerConfig Parameters

```kotlin
MediaPlayerConfig(
    autoPlay = true,                    // Start playback automatically
    showControls = true,                // Show player controls
    enableGestures = true,              // Enable touch gestures
    enableSpeedControl = true,          // Show speed selection
    enableQualitySelection = true,      // Show quality selection
    retryOnError = true,               // Enable automatic retry
    maxRetryCount = 3,                 // Maximum retry attempts
    autoQualityOnError = true,         // Auto-downgrade quality on errors
    
    // Playback speeds
    playbackSpeedOptions = listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f),
    
    // Glassy UI effects
    glassyUI = MediaPlayerConfig.GlassyUIConfig(
        blurRadius = 30f,
        borderOpacity = 0.4f
    ),
    
    // Audio visualization
    audioVisualization = MediaPlayerConfig.AudioVisualizationConfig(
        enableVisualization = true,
        colorScheme = VisualizationColorScheme.DYNAMIC
    )
)
```

### Audio Visualization Color Schemes

```kotlin
VisualizationColorScheme.DYNAMIC    // Colors change with audio
VisualizationColorScheme.BLUE       // Blue theme
VisualizationColorScheme.GREEN      // Green theme  
VisualizationColorScheme.RED        // Red theme
VisualizationColorScheme.PURPLE     // Purple theme
```

---

## Key Components

### ExoBoostPlayer
Ready-to-use video player with advanced controls and error handling.

### ExoBoostAudioPlayer  
Audio player with built-in equalizer and visualization effects.

### MediaPlayerConfig
Comprehensive configuration for customizing player behavior and UI.

---

## Error Handling

ExoBoost automatically handles common media playback errors:

- **Network interruptions**: Automatic retry with smart backoff
- **Codec failures**: Hardware to software decoder fallback  
- **Quality issues**: Automatic bitrate downgrading
- **SSL errors**: Clear error reporting with recovery options
- **Source problems**: Detailed error classification

All errors are handled internally with user-friendly fallbacks and recovery mechanisms.

---

## Requirements

- **Android API Level**: 24+ (Android 7.0)
- **Kotlin**: 1.8.0+
- **Jetpack Compose**: BOM 2024.02.00+
- **Media3**: 1.3.0+

---

## Support

- **GitHub**: [ExoBoostPlayer Repository](https://github.com/abbasiandev/ExoBoostPlayer)
- **Issues**: [Report Issues](https://github.com/abbasiandev/ExoBoostPlayer/issues)
- **Email**: info@abbasian.dev
- **Website**: https://abbasian.dev

---

*Built with ❤️ for the Android community*
