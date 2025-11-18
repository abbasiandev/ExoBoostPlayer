package dev.abbasian.exoboost.domain.usecase

import dev.abbasian.exoboost.data.repository.HighlightCacheRepository
import dev.abbasian.exoboost.domain.model.VideoHighlights
import dev.abbasian.exoboost.util.ExoBoostLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ManageHighlightCacheUseCase(
    private val repository: HighlightCacheRepository,
    private val logger: ExoBoostLogger,
) {
    companion object {
        private const val TAG = "ManageHighlightCacheUseCase"
    }

    suspend fun saveHighlight(
        videoUrl: String,
        highlights: VideoHighlights,
    ) = withContext(Dispatchers.IO) {
        try {
            repository.saveHighlight(videoUrl, highlights)
            logger.info(TAG, "Highlights saved for: $videoUrl")
        } catch (e: Exception) {
            logger.error(TAG, "Failed to save highlights", e)
        }
    }

    suspend fun getCachedHighlight(videoUrl: String): VideoHighlights? =
        withContext(Dispatchers.IO) {
            try {
                repository.getCachedHighlight(videoUrl)
            } catch (e: Exception) {
                logger.error(TAG, "Failed to get cached highlights", e)
                null
            }
        }

    fun getCachedHighlightFlow(videoUrl: String): Flow<VideoHighlights?> = repository.getCachedHighlightFlow(videoUrl)

    suspend fun deleteHighlight(videoUrl: String) =
        withContext(Dispatchers.IO) {
            try {
                repository.deleteHighlight(videoUrl)
                logger.info(TAG, "Deleted highlights for: $videoUrl")
            } catch (e: Exception) {
                logger.error(TAG, "Failed to delete highlights", e)
            }
        }

    suspend fun clearCache() =
        withContext(Dispatchers.IO) {
            try {
                repository.clearCache()
                logger.info(TAG, "Cache cleared successfully")
            } catch (e: Exception) {
                logger.error(TAG, "Failed to clear cache", e)
            }
        }

    fun getAllHighlights(): Flow<List<VideoHighlights>> = repository.getAllHighlights()

    suspend fun getRecentHighlights(limit: Int = 10): List<VideoHighlights> =
        withContext(Dispatchers.IO) {
            try {
                repository.getRecentHighlights(limit)
            } catch (e: Exception) {
                logger.error(TAG, "Failed to get recent highlights", e)
                emptyList()
            }
        }

    suspend fun hasHighlight(videoUrl: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                repository.hasHighlight(videoUrl)
            } catch (e: Exception) {
                logger.error(TAG, "Failed to check highlight existence", e)
                false
            }
        }

    suspend fun cleanExpiredCache() =
        withContext(Dispatchers.IO) {
            try {
                repository.cleanExpiredCache()
                logger.info(TAG, "Expired cache cleaned")
            } catch (e: Exception) {
                logger.error(TAG, "Failed to clean expired cache", e)
            }
        }
}
