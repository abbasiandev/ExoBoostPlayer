# ExoBoost

Enhanced ExoPlayer wrapper with intelligent error handling, automatic recovery, adaptive quality switching, and AI-powered video analysis including scene detection, motion tracking, audio analysis, face detection, automatic highlight generation, and chapter creation.

[![Maven Central](https://img.shields.io/maven-central/v/dev.abbasian/exoboost?color=blue)](https://search.maven.org/search?q=g:dev.abbasian%20AND%20a:exoboost)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Ready-brightgreen)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)
[![API Level](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://developer.android.com/guide/topics/manifest/uses-sdk-element#ApiLevels)

<p align="center">
  <img src="https://miro.medium.com/v2/resize:fit:720/format:webp/1*leuKdAStRv2ds58NUcz-5A.png" alt="Banner">
</p>

## AI-Powered Video Analysis(NEW)

- **Scene Detection**: Automatic detection of scene changes using histogram analysis
- **Motion Analysis**: Frame-by-frame motion intensity tracking and direction detection
- **Audio Analysis**: RMS-based audio level analysis with loudness detection
- **Highlight Generation**: Intelligent scoring and selection of video highlights
- **Chapter Generation**: Automatic video chapter creation based on scene boundaries
- **Parallel Processing**: Multi-threaded analysis for faster results
- **Adaptive Sampling**: Dynamic sampling rates based on video duration
- **Quick Mode**: Optimized analysis for faster processing on lower-end devices

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

## Installation

```toml
exoboost = "1.0.1-alpha09"
koin = "3.5.0" # same as library version needed

[libraries]
exoboost = { group = "dev.abbasian", name = "exoboost", version.ref = "exoboost" }

# Koin Dependencies
koin-android = { group = "io.insert-koin", name = "koin-android", version.ref = "koin" }
koin-androidx-compose = { group = "io.insert-koin", name = "koin-androidx-compose", version.ref = "koin" }
koin-core = { group = "io.insert-koin", name = "koin-core", version.ref = "koin" }
```

Add ExoBoost to your `build.gradle.kts`:

```gradle
dependencies {
    implementation(libs.exoboost)

     // Koin Dependencies
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
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

Manifest for Landscape Support and Permissions

Add to AndroidManifest.xml:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" /> <!-- For audio visualization -->

    <application
        android:name=".MyApplication">
        <activity
            android:name=".presentation.MainActivity"
            android:configChanges="orientation|screenSize"
            android:exported="true"
            android:theme="@style/Theme.ExoBoostPlayer">
        </activity>
    </application>
</manifest>
```

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
        onBack = { /* back navigation */ },
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
        onBack = { /* back navigation */ },
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
        onBack = { /* back navigation */ },
        modifier = Modifier.fillMaxSize()
    )
}
```

### Audio Playlist

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
        onBack = { /* back navigation */ },
        modifier = Modifier.fillMaxSize()
    )
}

data class Track(val url: String, val title: String, val artist: String)
```

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
    
    // Audio visualization
    audioVisualization = MediaPlayerConfig.AudioVisualizationConfig(
        enableVisualization = true,
        colorScheme = VisualizationColorScheme.DYNAMIC
    )
)
```

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

## Support

- **GitHub**: [ExoBoostPlayer Repository](https://github.com/abbasiandev/ExoBoostPlayer)
- **Issues**: [Report Issues](https://github.com/abbasiandev/ExoBoostPlayer/issues)
- **Email**: [info@abbasian.dev](mailto:info@abbasian.dev)
- **Portfolio**: https://abbasian.dev
