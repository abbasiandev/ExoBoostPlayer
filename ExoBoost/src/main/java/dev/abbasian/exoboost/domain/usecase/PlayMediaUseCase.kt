package dev.abbasian.exoboost.domain.usecase

import dev.abbasian.exoboost.domain.model.MediaPlayerConfig
import dev.abbasian.exoboost.domain.model.VideoQuality
import dev.abbasian.exoboost.domain.repository.MediaRepository

class PlayMediaUseCase(
    private val repository: MediaRepository
) {
    suspend fun execute(url: String, config: MediaPlayerConfig) {
        repository.loadMedia(url, config)
    }

    suspend fun applyEqualizerValues(values: List<Float>) {
        repository.applyEqualizerValues(values)
    }

    suspend fun getEqualizerBandCount(): Int {
        return repository.getEqualizerBandCount()
    }

    suspend fun getEqualizerFrequencies(): List<String> {
        return repository.getEqualizerFrequencies()
    }

    suspend fun play() = repository.play()

    suspend fun pause() = repository.pause()

    suspend fun setPlaybackSpeed(speed: Float) = repository.setPlaybackSpeed(speed)

    suspend fun selectQuality(quality: VideoQuality) = repository.selectQuality(quality)

    suspend fun getAvailableQualities(): List<VideoQuality> = repository.getAvailableQualities()

    suspend fun seekTo(position: Long) = repository.seekTo(position)

    suspend fun setVolume(volume: Float) = repository.setVolume(volume)

    fun release() = repository.release()
}