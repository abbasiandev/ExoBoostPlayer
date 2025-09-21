package dev.abbasian.exoboost.domain.repository

import dev.abbasian.exoboost.domain.model.VideoInfo
import dev.abbasian.exoboost.domain.model.VideoPlayerConfig
import dev.abbasian.exoboost.domain.model.VideoQuality
import dev.abbasian.exoboost.domain.model.VideoState
import kotlinx.coroutines.flow.Flow

interface VideoRepository {
    fun getVideoState(): Flow<VideoState>
    fun getVideoInfo(): Flow<VideoInfo>
    suspend fun loadVideo(url: String, config: VideoPlayerConfig)
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