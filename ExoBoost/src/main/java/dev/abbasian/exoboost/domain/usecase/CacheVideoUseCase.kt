package dev.abbasian.exoboost.domain.usecase

import dev.abbasian.exoboost.domain.repository.MediaRepository

class CacheVideoUseCase(
    private val repository: MediaRepository
) {
    suspend fun preloadVideo(url: String) {
       TODO()
    }

    suspend fun clearCache() {
        TODO()
    }
}