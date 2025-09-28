package dev.abbasian.exoboost.data.repository

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import dev.abbasian.exoboost.data.manager.CacheManager
import dev.abbasian.exoboost.data.manager.ExoPlayerManager
import dev.abbasian.exoboost.domain.model.MediaInfo
import dev.abbasian.exoboost.domain.model.MediaPlayerConfig
import dev.abbasian.exoboost.domain.model.VideoQuality
import dev.abbasian.exoboost.domain.model.MediaState
import dev.abbasian.exoboost.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow

@OptIn(UnstableApi::class)
class MediaRepositoryImpl(
    private val playerManager: ExoPlayerManager,
    private val cacheManager: CacheManager
) : MediaRepository {

    override fun getMediaState(): Flow<MediaState> = playerManager.mediaState

    override fun getMediaInfo(): Flow<MediaInfo> = playerManager.mediaInfo

    override suspend fun loadMedia(url: String, config: MediaPlayerConfig) {
        playerManager.initializePlayer(config)
        playerManager.loadMedia(url)
    }

    override suspend fun applyEqualizerValues(values: List<Float>) {
        playerManager.applyEqualizerValues(values)
    }

    override suspend fun getEqualizerBandCount(): Int {
        return playerManager.getEqualizerBandCount()
    }

    override suspend fun getEqualizerFrequencies(): List<String> {
        return playerManager.getEqualizerFrequencies()
    }

    override fun getEqualizerValues(): Flow<List<Float>> {
        return playerManager.equalizerValues
    }

    override suspend fun play() {
        playerManager.play()
    }

    override suspend fun pause() {
        playerManager.pause()
    }

    override suspend fun setPlaybackSpeed(speed: Float) {
        playerManager.setPlaybackSpeed(speed)
    }

    override suspend fun selectQuality(quality: VideoQuality) {
        playerManager.selectQuality(quality)
    }

    override suspend fun getAvailableQualities(): List<VideoQuality> {
        return playerManager.getAvailableQualities()
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