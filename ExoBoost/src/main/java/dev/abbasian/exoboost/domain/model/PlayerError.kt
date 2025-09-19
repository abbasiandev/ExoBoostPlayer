package dev.abbasian.exoboost.domain.model

sealed class PlayerError(
    open val message: String,
    open val cause: Throwable? = null
) {
    data class NetworkError(
        override val message: String,
        val isRetryable: Boolean = true,
        override val cause: Throwable? = null
    ) : PlayerError(message, cause)

    data class SSLError(
        override val message: String,
        val certificateInfo: String? = null,
        override val cause: Throwable? = null
    ) : PlayerError(message, cause)

    data class CodecError(
        override val message: String,
        val codecName: String? = null,
        override val cause: Throwable? = null
    ) : PlayerError(message, cause)

    data class SourceError(
        override val message: String,
        val sourceUrl: String? = null,
        override val cause: Throwable? = null
    ) : PlayerError(message, cause)

    data class LiveStreamError(
        override val message: String,
        val httpCode: Int? = null,
        override val cause: Throwable? = null
    ) : PlayerError(message, cause)

    data class UnknownError(
        override val message: String,
        override val cause: Throwable? = null
    ) : PlayerError(message, cause)
}