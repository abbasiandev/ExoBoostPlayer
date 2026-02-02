package dev.abbasian.exoboost.domain.model

data class SubtitleInfo(
    val availableTracks: List<SubtitleTrack> = emptyList(),
    val currentTrack: SubtitleTrack? = null,
    val isEnabled: Boolean = false,
    val style: SubtitleStyle = SubtitleStyle(),
)
