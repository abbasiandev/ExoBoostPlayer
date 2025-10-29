package dev.abbasian.exoboost.data.manager

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import dev.abbasian.exoboost.util.ExoBoostLogger
import java.io.File

@UnstableApi
class CacheManager(
    private val context: Context,
    private val logger: ExoBoostLogger,
) {
    companion object {
        private const val TAG = "CacheManager"
    }

    private val cacheSize = 100L * 1024 * 1024 // 100MB
    private val cacheDir = File(context.cacheDir, "video_cache")

    private val internalCache: Cache by lazy {
        SimpleCache(
            cacheDir,
            LeastRecentlyUsedCacheEvictor(cacheSize),
            StandaloneDatabaseProvider(context),
        )
    }

    fun getCache(): Cache = internalCache

    fun clearCache() {
        try {
            internalCache.keys.forEach { key ->
                internalCache.removeResource(key)
            }
        } catch (e: Exception) {
            logger.error(TAG, "Error clearing cache", e)
        }
    }

    fun getCacheSize(): Long = internalCache.cacheSpace

    fun release() {
        try {
            internalCache.release()
        } catch (e: Exception) {
            logger.error(TAG, "Error releasing cache", e)
        }
    }
}
