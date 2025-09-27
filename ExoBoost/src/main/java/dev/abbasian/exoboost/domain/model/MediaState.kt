package dev.abbasian.exoboost.domain.model

sealed class MediaState {
    object Idle : MediaState()
    object Loading : MediaState()
    object Ready : MediaState()
    object Playing : MediaState()
    object Paused : MediaState()
    object Ended : MediaState()
    data class Error(val error: PlayerError) : MediaState()
}

data class MediaInfo(
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val bufferedPosition: Long = 0L,
    val volume: Float = 1f,
    val playbackSpeed: Float = 1.0f,
    val isPlaying: Boolean = false,
    val hasVideo: Boolean = true,
    val hasAudio: Boolean = true,
    val availableQualities: List<VideoQuality> = emptyList(),
    val currentQuality: VideoQuality? = null
)