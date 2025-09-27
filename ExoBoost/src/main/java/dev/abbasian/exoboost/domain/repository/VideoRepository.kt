package dev.abbasian.exoboost.domain.repository

import dev.abbasian.exoboost.domain.model.MediaInfo
import dev.abbasian.exoboost.domain.model.MediaPlayerConfig
import dev.abbasian.exoboost.domain.model.MediaState
import dev.abbasian.exoboost.domain.model.VideoQuality
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getMediaState(): Flow<MediaState>
    fun getMediaInfo(): Flow<MediaInfo>
    suspend fun loadMedia(url: String, config: MediaPlayerConfig)
    suspend fun play()
    suspend fun pause()
    suspend fun setPlaybackSpeed(speed: Float)
    suspend fun selectQuality(quality: VideoQuality)
    suspend fun getAvailableQualities(): List<VideoQuality>
    suspend fun seekTo(position: Long)
    suspend fun setVolume(volume: Float)
    suspend fun retry()
    fun release()
}