package dev.abbasian.exoboost.domain.usecase

import dev.abbasian.exoboost.domain.model.VideoPlayerConfig
import dev.abbasian.exoboost.domain.repository.VideoRepository

class PlayVideoUseCase(
    private val repository: VideoRepository
) {
    suspend fun execute(url: String, config: VideoPlayerConfig) {
        repository.loadVideo(url, config)
    }

    suspend fun play() = repository.play()

    suspend fun pause() = repository.pause()

    suspend fun seekTo(position: Long) = repository.seekTo(position)

    suspend fun setVolume(volume: Float) = repository.setVolume(volume)

    fun release() = repository.release()
}