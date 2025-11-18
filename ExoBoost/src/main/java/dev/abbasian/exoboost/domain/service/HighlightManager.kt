package dev.abbasian.exoboost.domain.service

import android.content.Context
import dev.abbasian.exoboost.domain.model.HighlightConfig
import dev.abbasian.exoboost.domain.model.VideoHighlights
import dev.abbasian.exoboost.domain.usecase.ManageHighlightCacheUseCase
import dev.abbasian.exoboost.util.ExoBoostLogger
import kotlinx.coroutines.flow.Flow

class HighlightManager(
    private val cacheUseCase: ManageHighlightCacheUseCase,
    private val logger: ExoBoostLogger,
) {
    companion object {
        private const val TAG = "HighlightManager"
    }

    fun generateHighlights(
        context: Context,
        videoUrl: String,
        config: HighlightConfig = HighlightConfig(),
        useCache: Boolean = true,
        useBackgroundService: Boolean = true,
    ) {
        logger.info(TAG, "Generating highlights for: $videoUrl")

        if (useBackgroundService) {
            HighlightGenerationService.startService(
                context = context,
                videoUrl = videoUrl,
                config = config,
            )
        } else {
            logger.warning(TAG, "In-memory generation should be handled by ViewModel")
        }
    }

    suspend fun getCachedHighlights(videoUrl: String): VideoHighlights? {
        logger.debug(TAG, "Getting cached highlights for: $videoUrl")
        return cacheUseCase.getCachedHighlight(videoUrl)
    }

    fun getCachedHighlightsFlow(videoUrl: String): Flow<VideoHighlights?> {
        return cacheUseCase.getCachedHighlightFlow(videoUrl)
    }

    suspend fun hasHighlights(videoUrl: String): Boolean {
        return cacheUseCase.hasHighlight(videoUrl)
    }

    suspend fun deleteHighlights(videoUrl: String) {
        logger.info(TAG, "Deleting highlights for: $videoUrl")
        cacheUseCase.deleteHighlight(videoUrl)
    }

    suspend fun clearAllHighlights() {
        logger.info(TAG, "Clearing all highlights")
        cacheUseCase.clearCache()
    }

    suspend fun getRecentHighlights(limit: Int = 10): List<VideoHighlights> {
        return cacheUseCase.getRecentHighlights(limit)
    }

    fun getAllHighlights(): Flow<List<VideoHighlights>> {
        return cacheUseCase.getAllHighlights()
    }

    suspend fun cleanExpiredCache() {
        logger.info(TAG, "Cleaning expired cache")
        cacheUseCase.cleanExpiredCache()
    }

    suspend fun getHighlightStats(): HighlightStats {
        val all = cacheUseCase.getRecentHighlights(Int.MAX_VALUE)
        val totalVideos = all.size
        val totalSegments = all.sumOf { it.highlights.size }
        val totalDuration = all.sumOf { it.highlightDuration }
        val avgConfidence = if (all.isNotEmpty()) {
            all.map { it.confidenceScore }.average().toFloat()
        } else {
            0f
        }

        return HighlightStats(
            totalVideos = totalVideos,
            totalSegments = totalSegments,
            totalDurationMs = totalDuration,
            averageConfidence = avgConfidence,
        )
    }
}

data class HighlightStats(
    val totalVideos: Int,
    val totalSegments: Int,
    val totalDurationMs: Long,
    val averageConfidence: Float,
)