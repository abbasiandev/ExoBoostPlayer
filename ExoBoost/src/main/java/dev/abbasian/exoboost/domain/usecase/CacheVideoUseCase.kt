package dev.abbasian.exoboost.domain.usecase

import dev.abbasian.exoboost.domain.repository.VideoRepository

class CacheVideoUseCase(
    private val repository: VideoRepository
) {
    suspend fun preloadVideo(url: String) {
       TODO()
    }

    suspend fun clearCache() {
        TODO()
    }
}