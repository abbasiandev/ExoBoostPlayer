package dev.abbasian.exoboost.domain.model

data class SubtitleConfig(
    val autoLoadSubtitles: Boolean = true,
    val preferredLanguages: List<String> = listOf("en", "fa", "ar"),
    val defaultStyle: SubtitleStyle = SubtitleStyle(),
    val enableSubtitleSearch: Boolean = true,
    val cacheSubtitles: Boolean = true,
    val maxCacheSize: Long = 50 * 1024 * 1024, // 50MB
)

sealed class SubtitleState {
    object Disabled : SubtitleState()

    object Loading : SubtitleState()

    data class Loaded(
        val track: SubtitleTrack,
    ) : SubtitleState()

    data class Error(
        val message: String,
    ) : SubtitleState()
}
