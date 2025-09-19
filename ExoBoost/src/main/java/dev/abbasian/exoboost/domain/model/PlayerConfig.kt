package dev.abbasian.exoboost.domain.model

data class VideoPlayerConfig(
    val enableCache: Boolean = true,
    val autoPlay: Boolean = true,
    val showControls: Boolean = true,
    val enableGestures: Boolean = true,
    val retryOnError: Boolean = true,
    val maxRetryCount: Int = 3,
    val bufferDurations: BufferDurations = BufferDurations()
) {
    data class BufferDurations(
        val minBufferMs: Int = 15000,
        val maxBufferMs: Int = 50000,
        val bufferForPlaybackMs: Int = 2500,
        val bufferForPlaybackAfterRebufferMs: Int = 5000
    )
}