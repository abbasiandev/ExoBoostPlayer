package dev.abbasian.exoboost.domain.usecase

import dev.abbasian.exoboost.domain.repository.MediaRepository

class RetryVideoUseCase(
    private val repository: MediaRepository
) {
    suspend fun execute() {
        repository.retry()
    }
}