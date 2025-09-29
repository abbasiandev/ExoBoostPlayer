package dev.abbasian.exoboost.domain.model

sealed class PlayerError(
    open val message: String,
    open val cause: Throwable? = null,
    open val errorCode: String
) {
    data class NetworkError(
        override val message: String,
        override val cause: Throwable? = null,
        override val errorCode: String = "NETWORK_ERROR",
        val retryable: Boolean = true
    ) : PlayerError(message, cause, errorCode)

    data class SSLError(
        override val message: String,
        val certificateInfo: String? = null,
        override val cause: Throwable? = null,
        override val errorCode: String = "SSL_ERROR"
    ) : PlayerError(message, cause, errorCode)

    data class CodecError(
        override val message: String,
        val codecName: String? = null,
        val mimeType: String? = null,
        override val cause: Throwable? = null,
        override val errorCode: String = "CODEC_ERROR"
    ) : PlayerError(message, cause, errorCode)

    data class SourceError(
        override val message: String,
        val sourceUrl: String? = null,
        override val cause: Throwable? = null,
        override val errorCode: String = "SOURCE_ERROR"
    ) : PlayerError(message, cause, errorCode)

    data class LiveStreamError(
        override val message: String,
        val httpCode: Int? = null,
        override val cause: Throwable? = null,
        override val errorCode: String = "LIVE_STREAM_ERROR"
    ) : PlayerError(message, cause, errorCode)

    data class TimeoutError(
        override val message: String,
        val timeoutMs: Long? = null,
        override val cause: Throwable? = null,
        override val errorCode: String = "TIMEOUT_ERROR"
    ) : PlayerError(message, cause, errorCode)

    data class DrmError(
        override val message: String,
        val drmScheme: String? = null,
        override val cause: Throwable? = null,
        override val errorCode: String = "DRM_ERROR"
    ) : PlayerError(message, cause, errorCode)

    data class UnknownError(
        override val message: String,
        override val cause: Throwable? = null,
        override val errorCode: String = "UNKNOWN_ERROR"
    ) : PlayerError(message, cause, errorCode)

    open val isRetryable: Boolean
        get() = when (this) {
            is NetworkError -> retryable
            is TimeoutError -> true
            is LiveStreamError -> httpCode in listOf(500, 502, 503, 504)
            is SourceError -> true
            is UnknownError -> true
            else -> false
        }

    val severity: ErrorSeverity
        get() = when (this) {
            is SSLError -> ErrorSeverity.CRITICAL
            is DrmError -> ErrorSeverity.CRITICAL
            is CodecError -> ErrorSeverity.HIGH
            is LiveStreamError -> when (httpCode) {
                403, 404 -> ErrorSeverity.HIGH
                else -> ErrorSeverity.MEDIUM
            }
            is NetworkError -> if (isRetryable) ErrorSeverity.LOW else ErrorSeverity.MEDIUM
            is TimeoutError -> ErrorSeverity.LOW
            else -> ErrorSeverity.MEDIUM
        }
}

enum class ErrorSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}