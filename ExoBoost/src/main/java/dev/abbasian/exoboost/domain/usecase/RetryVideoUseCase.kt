package dev.abbasian.exoboost.domain.usecase

import dev.abbasian.exoboost.domain.repository.VideoRepository

class RetryVideoUseCase(
    private val repository: VideoRepository
) {
    suspend fun execute() {
        repository.retry()
    }
}