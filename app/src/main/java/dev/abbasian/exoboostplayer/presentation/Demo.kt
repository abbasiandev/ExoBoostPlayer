package dev.abbasian.exoboostplayer.presentation

sealed class Demo {
    data class VideoBasic(val url: String) : Demo()
    data class VideoAdvanced(val url: String) : Demo()
    data class VideoErrorRecovery(val url: String) : Demo()
    data class VideoQualityControl(val url: String) : Demo()
    data class AudioVisualization(val url: String, val title: String, val artist: String) : Demo()
    data class AudioEqualizer(val url: String, val title: String, val artist: String) : Demo()
    object AudioPlaylist : Demo()
}

data class Track(val url: String, val title: String, val artist: String)