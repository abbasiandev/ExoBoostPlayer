package dev.abbasian.exoboostplayer.presentation

import androidx.compose.ui.graphics.vector.ImageVector
import dev.abbasian.exoboost.domain.model.VideoQuality

sealed class Demo {
    data class VideoBasic(val url: String) : Demo()
    data class VideoAdvanced(val url: String) : Demo()
    data class VideoErrorRecovery(val url: String) : Demo()
    data class VideoQualityControl(val url: String) : Demo()
    data class ErrorComparison(val url: String) : Demo()
    data class BufferVisualization(val url: String) : Demo()
    data class CacheDemo(val url: String) : Demo()
    data class NetworkSimulation(val url: String) : Demo()

    data class AudioVisualization(val url: String, val title: String, val artist: String) : Demo()
    data class AudioEqualizer(val url: String, val title: String, val artist: String) : Demo()
    object AudioPlaylist : Demo()

    data class AIThumbnails(val url: String) : Demo()
    data class VideoAnalysis(val url: String) : Demo()
}

data class Track(val url: String, val title: String, val artist: String)

data class DemoCategory(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val demos: List<DemoItem>
)

data class DemoItem(
    val title: String,
    val description: String,
    val features: List<String>,
    val difficulty: DemoDifficulty,
    val demo: Demo
)

data class RetryAttempt(
    val attemptNumber: Int,
    val timestamp: Long,
    val delayMs: Long,
    val success: Boolean
)

data class QualityChangeEvent(
    val fromQuality: VideoQuality?,
    val toQuality: VideoQuality,
    val timestamp: Long,
    val reason: String
)

data class PlayerMetrics(
    val playerName: String,
    val errorCount: Int = 0,
    val recoveryCount: Int = 0,
    val failureCount: Int = 0,
    val totalDowntime: Long = 0,
    val averageRecoveryTime: Long = 0
)

data class BufferSnapshot(
    val timestamp: Long,
    val bufferedPosition: Long,
    val currentPosition: Long,
    val bufferPercentage: Float
)

enum class NetworkSimulation {
    PERFECT, GOOD, MODERATE, POOR, VERY_POOR, OFFLINE
}

enum class DemoDifficulty {
    BEGINNER, INTERMEDIATE, ADVANCED
}