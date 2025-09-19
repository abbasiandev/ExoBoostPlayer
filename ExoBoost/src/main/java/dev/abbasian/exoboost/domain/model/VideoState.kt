package dev.abbasian.exoboost.domain.model

sealed class VideoState {
    object Idle : VideoState()
    object Loading : VideoState()
    object Ready : VideoState()
    object Playing : VideoState()
    object Paused : VideoState()
    object Ended : VideoState()
    data class Error(val error: PlayerError) : VideoState()
}

data class VideoInfo(
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val bufferedPosition: Long = 0L,
    val volume: Float = 1f,
    val playbackSpeed: Float = 1f,
    val isPlaying: Boolean = false,
    val hasVideo: Boolean = true,
    val hasAudio: Boolean = true
)