package dev.abbasian.exoboost.data.repository

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import dev.abbasian.exoboost.data.manager.CacheManager
import dev.abbasian.exoboost.data.manager.ExoPlayerManager
import dev.abbasian.exoboost.domain.model.VideoInfo
import dev.abbasian.exoboost.domain.model.VideoPlayerConfig
import dev.abbasian.exoboost.domain.model.VideoState
import dev.abbasian.exoboost.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow

@OptIn(UnstableApi::class)
class VideoRepositoryImpl(
    private val playerManager: ExoPlayerManager,
    private val cacheManager: CacheManager
) : VideoRepository {

    override fun getVideoState(): Flow<VideoState> = playerManager.videoState

    override fun getVideoInfo(): Flow<VideoInfo> = playerManager.videoInfo

    override suspend fun loadVideo(url: String, config: VideoPlayerConfig) {
        playerManager.initializePlayer(config)
        playerManager.loadVideo(url)
    }

    override suspend fun play() {
        playerManager.play()
    }

    override suspend fun pause() {
        playerManager.pause()
    }

    override suspend fun seekTo(position: Long) {
        playerManager.seekTo(position)
    }

    override suspend fun setVolume(volume: Float) {
        playerManager.setVolume(volume)
    }

    override suspend fun retry() {
        playerManager.retry()
    }

    override fun release() {
        playerManager.release()
    }
}