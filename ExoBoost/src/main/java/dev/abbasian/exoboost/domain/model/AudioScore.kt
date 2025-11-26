package dev.abbasian.exoboost.domain.model

data class AudioScore(
    val timestampMs: Long,
    val volumeLevel: Float,
    val isLoud: Boolean,
)
