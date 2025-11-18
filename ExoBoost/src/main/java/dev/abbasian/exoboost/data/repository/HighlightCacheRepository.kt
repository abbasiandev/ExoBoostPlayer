package dev.abbasian.exoboost.data.repository

import dev.abbasian.exoboost.data.local.dao.VideoHighlightDao
import dev.abbasian.exoboost.data.local.store.HighlightPreferences
import dev.abbasian.exoboost.data.mapper.toDomain
import dev.abbasian.exoboost.data.mapper.toEntity
import dev.abbasian.exoboost.domain.model.VideoHighlights
import dev.abbasian.exoboost.util.ExoBoostLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

class HighlightCacheRepository(
    private val highlightDao: VideoHighlightDao,
    private val preferences: HighlightPreferences,
    private val logger: ExoBoostLogger,
) {
    companion object {
        private const val TAG = "HighlightCacheRepository"
    }

    suspend fun getCachedHighlight(videoUrl: String): VideoHighlights? {
        return try {
            val enableCache = preferences.enableCache.first()
            if (!enableCache) {
                logger.debug(TAG, "Cache is disabled")
                return null
            }

            val entity = highlightDao.getHighlight(videoUrl)
            if (entity != null) {
                highlightDao.updateAccessInfo(videoUrl)

                val expiryDays = preferences.cacheExpiryDays.first()
                val expiryTime =
                    System.currentTimeMillis() - TimeUnit.DAYS.toMillis(expiryDays.toLong())

                if (entity.generatedAt < expiryTime) {
                    logger.info(TAG, "Cache expired for: $videoUrl")
                    highlightDao.deleteHighlight(videoUrl)
                    return null
                }

                logger.info(TAG, "Cache hit for: $videoUrl")
                entity.toDomain()
            } else {
                logger.debug(TAG, "Cache miss for: $videoUrl")
                null
            }
        } catch (e: Exception) {
            logger.error(TAG, "Error getting cached highlight", e)
            null
        }
    }

    fun getCachedHighlightFlow(videoUrl: String): Flow<VideoHighlights?> =
        highlightDao.getHighlightFlow(videoUrl).map { entity ->
            entity?.toDomain()
        }

    suspend fun saveHighlight(
        videoUrl: String,
        highlights: VideoHighlights,
    ) {
        try {
            val enableCache = preferences.enableCache.first()
            if (!enableCache) {
                logger.debug(TAG, "Cache is disabled, not saving")
                return
            }

            val maxSize = preferences.maxCacheSize.first()
            val currentCount = highlightDao.getHighlightCount()

            if (currentCount >= maxSize) {
                // Remove oldest entries
                val toRemove = currentCount - maxSize + 1
                logger.info(TAG, "Cache full, removing $toRemove oldest entries")
                val expiryTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
                highlightDao.deleteOldHighlights(expiryTime)
            }

            val entity = highlights.toEntity(videoUrl)
            highlightDao.insertHighlight(entity)

            preferences.recordHighlightGeneration(videoUrl)

            logger.info(TAG, "Saved highlight for: $videoUrl")
        } catch (e: Exception) {
            logger.error(TAG, "Error saving highlight", e)
        }
    }

    suspend fun deleteHighlight(videoUrl: String) {
        try {
            highlightDao.deleteHighlight(videoUrl)
            logger.info(TAG, "Deleted highlight for: $videoUrl")
        } catch (e: Exception) {
            logger.error(TAG, "Error deleting highlight", e)
        }
    }

    fun getAllHighlights(): Flow<List<VideoHighlights>> =
        highlightDao.getAllHighlights().map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getRecentHighlights(limit: Int = 10): List<VideoHighlights> =
        try {
            highlightDao.getRecentHighlights(limit).map { it.toDomain() }
        } catch (e: Exception) {
            logger.error(TAG, "Error getting recent highlights", e)
            emptyList()
        }

    suspend fun hasHighlight(videoUrl: String): Boolean =
        try {
            highlightDao.hasHighlight(videoUrl)
        } catch (e: Exception) {
            logger.error(TAG, "Error checking highlight existence", e)
            false
        }

    suspend fun clearCache() {
        try {
            highlightDao.deleteAllHighlights()
            logger.info(TAG, "Cache cleared")
        } catch (e: Exception) {
            logger.error(TAG, "Error clearing cache", e)
        }
    }

    suspend fun cleanExpiredCache() {
        try {
            val expiryDays = preferences.cacheExpiryDays.first()
            val expiryTime =
                System.currentTimeMillis() - TimeUnit.DAYS.toMillis(expiryDays.toLong())
            highlightDao.deleteOldHighlights(expiryTime)
            logger.info(TAG, "Cleaned expired cache")
        } catch (e: Exception) {
            logger.error(TAG, "Error cleaning expired cache", e)
        }
    }
}
