package dev.abbasian.exoboost.data.manager

import dev.abbasian.exoboost.domain.model.PlayerError
import dev.abbasian.exoboost.util.ExoBoostLogger
import kotlinx.coroutines.delay

class AutoRecoveryManager(
    private val logger: ExoBoostLogger
) {
    companion object {
        private const val TAG = "AutoRecoveryManager"
        private const val INITIAL_RETRY_DELAY = 1000L
        private const val MAX_RETRY_DELAY = 10000L
    }

    private var retryAttempts = 0

    suspend fun shouldRetry(error: PlayerError, maxAttempts: Int): Boolean {
        return when (error) {
            is PlayerError.NetworkError -> {
                if (error.isRetryable && retryAttempts < maxAttempts) {
                    retryAttempts++
                    val delayMs = calculateBackoff()
                    logger.info(TAG, "Auto-retry attempt $retryAttempts/$maxAttempts after ${delayMs}ms")
                    delay(delayMs)
                    true
                } else false
            }
            is PlayerError.LiveStreamError -> {
                if (error.httpCode in listOf(500, 502, 503, 504) && retryAttempts < maxAttempts) {
                    retryAttempts++
                    delay(calculateBackoff())
                    true
                } else false
            }
            else -> false
        }
    }

    private fun calculateBackoff(): Long {
        return (INITIAL_RETRY_DELAY * (1 shl (retryAttempts - 1)))
            .coerceAtMost(MAX_RETRY_DELAY)
    }

    fun reset() {
        retryAttempts = 0
    }
}