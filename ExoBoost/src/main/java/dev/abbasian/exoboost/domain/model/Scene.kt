package dev.abbasian.exoboost.domain.model

data class Scene(
    val startMs: Long,
    val endMs: Long,
    val averageBrightness: Float,
    val averageMotion: Float,
    val changeIntensity: Float,
)
