package dev.abbasian.exoboost.domain.model

data class MediaPlayerConfig(
    val enableCache: Boolean = true,
    val autoPlay: Boolean = true,
    val showControls: Boolean = true,
    val enableGestures: Boolean = true,
    val retryOnError: Boolean = true,
    val maxRetryCount: Int = 3,
    val bufferDurations: BufferDurations = BufferDurations(),
    val playbackSpeedOptions: List<Float> = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f),
    val defaultPlaybackSpeed: Float = 1.0f,
    val enableSpeedControl: Boolean = true,
    val enableQualitySelection: Boolean = true,
    val autoQualityOnError: Boolean = true,
    val preferSoftwareDecoder: Boolean = false,

    val audioVisualization: AudioVisualizationConfig = AudioVisualizationConfig(),
    val glassyUI: GlassyUIConfig = GlassyUIConfig()
) {
    data class BufferDurations(
        val minBufferMs: Int = 15000,
        val maxBufferMs: Int = 50000,
        val bufferForPlaybackMs: Int = 2500,
        val bufferForPlaybackAfterRebufferMs: Int = 5000
    )

    data class AudioVisualizationConfig(
        val enableVisualization: Boolean = true,
        val visualizationType: VisualizationType = VisualizationType.SPECTRUM,
        val sensitivity: Float = 1.0f,
        val colorScheme: VisualizationColorScheme = VisualizationColorScheme.DYNAMIC,
        val smoothingFactor: Float = 1.0f
    )

    data class GlassyUIConfig(
        val blurRadius: Float? = null,
        val backgroundBlur: Float = 20f,
        val backgroundOpacity: Float = 0.1f,
        val borderOpacity: Float = 0.3f,
        val shadowEnabled: Boolean = true,
        val animationDuration: Int = 300,
        val cornerRadius: Float? = null
    )
}

enum class VisualizationType {
    SPECTRUM, WAVEFORM, CIRCULAR, BARS, PARTICLE_SYSTEM
}

enum class VisualizationColorScheme {
    DYNAMIC, MONOCHROME, RAINBOW, MATERIAL_YOU
}