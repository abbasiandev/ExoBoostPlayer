package dev.abbasian.exoboost.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.abbasian.exoboost.data.local.VideoHighlightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoHighlightDao {
    @Query("SELECT * FROM video_highlights WHERE videoUrl = :videoUrl")
    suspend fun getHighlight(videoUrl: String): VideoHighlightEntity?

    @Query("SELECT * FROM video_highlights WHERE videoUrl = :videoUrl")
    fun getHighlightFlow(videoUrl: String): Flow<VideoHighlightEntity?>

    @Query("SELECT * FROM video_highlights ORDER BY lastAccessedAt DESC")
    fun getAllHighlights(): Flow<List<VideoHighlightEntity>>

    @Query("SELECT * FROM video_highlights ORDER BY lastAccessedAt DESC LIMIT :limit")
    suspend fun getRecentHighlights(limit: Int = 10): List<VideoHighlightEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: VideoHighlightEntity)

    @Update
    suspend fun updateHighlight(highlight: VideoHighlightEntity)

    @Query("UPDATE video_highlights SET lastAccessedAt = :timestamp, accessCount = accessCount + 1 WHERE videoUrl = :videoUrl")
    suspend fun updateAccessInfo(
        videoUrl: String,
        timestamp: Long = System.currentTimeMillis(),
    )

    @Query("DELETE FROM video_highlights WHERE videoUrl = :videoUrl")
    suspend fun deleteHighlight(videoUrl: String)

    @Query("DELETE FROM video_highlights")
    suspend fun deleteAllHighlights()

    @Query("DELETE FROM video_highlights WHERE lastAccessedAt < :timestamp")
    suspend fun deleteOldHighlights(timestamp: Long)

    @Query("SELECT COUNT(*) FROM video_highlights")
    suspend fun getHighlightCount(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM video_highlights WHERE videoUrl = :videoUrl)")
    suspend fun hasHighlight(videoUrl: String): Boolean
}
