package dev.abbasian.exoboost.domain.model

sealed class PlayerError(
    open val message: String,
    open val cause: Throwable? = null,
    open val errorCode: String,
) {
    data class NetworkError(
        override val message: String,
        override val cause: Throwable? = null,
        override val errorCode: String = "NETWORK_ERROR",
        val retryable: Boolean = true,
        val networkErrorType: NetworkErrorType = NetworkErrorType.GENERIC,
    ) : PlayerError(message, cause, errorCode)

    enum class NetworkErrorType {
        GENERIC,
        DNS_RESOLUTION_FAILED,
        CONNECTION_TIMEOUT,
        CONNECTION_REFUSED,
        CONNECTION_RESET,
        NO_INTERNET,
    }

    data class SSLError(
        override val message: String,
        val certificateInfo: String? = null,
        override val cause: Throwable? = null,
        override val errorCode: String = "SSL_ERROR",
    ) : PlayerError(message, cause, errorCode)

    data class CodecError(
        override val message: String,
        val codecName: String? = null,
        val mimeType: String? = null,
        val exoPlayerErrorCode: Int? = null,
        override val cause: Throwable? = null,
        override val errorCode: String = "CODEC_ERROR",
        val codecErrorType: CodecErrorType = CodecErrorType.GENERIC,
    ) : PlayerError(message, cause, errorCode)

    enum class CodecErrorType {
        GENERIC,
        DECODER_NOT_AVAILABLE,
        DECODER_INIT_FAILED,
        DECODING_FAILED,
        FORMAT_UNSUPPORTED,
    }

    data class SourceError(
        override val message: String,
        val sourceUrl: String? = null,
        override val cause: Throwable? = null,
        override val errorCode: String = "SOURCE_ERROR",
        val sourceErrorType: SourceErrorType = SourceErrorType.GENERIC,
    ) : PlayerError(message, cause, errorCode)

    enum class SourceErrorType {
        GENERIC,
        FILE_NOT_FOUND,
        FILE_CORRUPTED,
        MANIFEST_MALFORMED,
        CONTAINER_MALFORMED,
        UNSUPPORTED_FORMAT,
        CORS_ERROR,
    }

    data class LiveStreamError(
        override val message: String,
        val httpCode: Int? = null,
        override val cause: Throwable? = null,
        override val errorCode: String = "LIVE_STREAM_ERROR",
    ) : PlayerError(message, cause, errorCode)

    data class TimeoutError(
        override val message: String,
        val timeoutMs: Long? = null,
        override val cause: Throwable? = null,
        override val errorCode: String = "TIMEOUT_ERROR",
    ) : PlayerError(message, cause, errorCode)

    data class DrmError(
        override val message: String,
        val drmScheme: String? = null,
        override val cause: Throwable? = null,
        override val errorCode: String = "DRM_ERROR",
        val drmErrorType: DrmErrorType = DrmErrorType.GENERIC,
    ) : PlayerError(message, cause, errorCode)

    enum class DrmErrorType {
        GENERIC,
        LICENSE_ACQUISITION_FAILED,
        PROVISIONING_FAILED,
        DEVICE_REVOKED,
        LICENSE_EXPIRED,
    }

    data class UnknownError(
        override val message: String,
        override val cause: Throwable? = null,
        override val errorCode: String = "UNKNOWN_ERROR",
    ) : PlayerError(message, cause, errorCode)

    open val isRetryable: Boolean
        get() =
            when (this) {
                is NetworkError -> retryable
                is TimeoutError -> true
                is LiveStreamError -> httpCode in listOf(500, 502, 503, 504)
                is SourceError ->
                    sourceErrorType in
                        listOf(
                            SourceErrorType.FILE_NOT_FOUND,
                            SourceErrorType.MANIFEST_MALFORMED,
                        )

                is CodecError -> codecErrorType == CodecErrorType.DECODER_INIT_FAILED
                is UnknownError -> true
                else -> false
            }

    val severity: ErrorSeverity
        get() =
            when (this) {
                is SSLError -> ErrorSeverity.CRITICAL
                is DrmError -> ErrorSeverity.CRITICAL
                is CodecError ->
                    when (codecErrorType) {
                        CodecErrorType.DECODER_NOT_AVAILABLE -> ErrorSeverity.CRITICAL
                        CodecErrorType.FORMAT_UNSUPPORTED -> ErrorSeverity.HIGH
                        else -> ErrorSeverity.MEDIUM
                    }

                is SourceError ->
                    when (sourceErrorType) {
                        SourceErrorType.FILE_CORRUPTED -> ErrorSeverity.HIGH
                        SourceErrorType.UNSUPPORTED_FORMAT -> ErrorSeverity.HIGH
                        else -> ErrorSeverity.MEDIUM
                    }

                is LiveStreamError ->
                    when (httpCode) {
                        403, 404 -> ErrorSeverity.HIGH
                        else -> ErrorSeverity.MEDIUM
                    }

                is NetworkError ->
                    when (networkErrorType) {
                        NetworkErrorType.NO_INTERNET -> ErrorSeverity.MEDIUM
                        NetworkErrorType.DNS_RESOLUTION_FAILED -> ErrorSeverity.MEDIUM
                        else -> if (isRetryable) ErrorSeverity.LOW else ErrorSeverity.MEDIUM
                    }

                is TimeoutError -> ErrorSeverity.LOW
                else -> ErrorSeverity.MEDIUM
            }
}

enum class ErrorSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}
