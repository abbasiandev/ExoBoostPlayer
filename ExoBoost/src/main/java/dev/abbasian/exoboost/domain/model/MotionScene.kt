package dev.abbasian.exoboost.domain.model

data class MotionScore(
    val timestampMs: Long,
    val motionIntensity: Float,
    val direction: MotionDirection,
)

enum class MotionDirection {
    NONE,
    MINIMAL,
    MODERATE,
    HIGH,
    EXTREME,
}
