package dev.abbasian.exoboost.domain.error

import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import dev.abbasian.exoboost.domain.model.PlayerError
import dev.abbasian.exoboost.util.ExoBoostLogger
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

@UnstableApi
class ErrorClassifier(
    private val logger: ExoBoostLogger
) {
    companion object {
        private const val TAG = "ErrorClassifier"
    }

    fun classifyError(exception: Throwable): PlayerError {
        logger.debug(TAG, "Classifying error: ${exception.javaClass.simpleName}")

        return when (exception) {
            is PlaybackException -> classifyPlaybackException(exception)
            is IOException -> classifyIOException(exception)
            is SSLException -> classifySSLException(exception)
            else -> PlayerError.UnknownError(
                message = exception.message ?: "Unknown error occurred",
                cause = exception
            )
        }
    }

    private fun classifyPlaybackException(exception: PlaybackException): PlayerError {
        return when (exception.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> {
                val networkType = detectNetworkErrorType(exception)
                PlayerError.NetworkError(
                    message = getNetworkErrorMessage(networkType),
                    cause = exception,
                    retryable = true,
                    networkErrorType = networkType
                )
            }

            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> {
                PlayerError.NetworkError(
                    message = "Network connection timed out",
                    cause = exception,
                    retryable = true,
                    networkErrorType = PlayerError.NetworkErrorType.CONNECTION_TIMEOUT
                )
            }

            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> {
                val httpException = exception.cause as? HttpDataSource.InvalidResponseCodeException

                if (isCorsError(httpException)) {
                    return PlayerError.SourceError(
                        message = "CORS policy blocking access to media",
                        sourceUrl = httpException?.dataSpec?.uri?.toString(),
                        cause = exception,
                        sourceErrorType = PlayerError.SourceErrorType.CORS_ERROR
                    )
                }

                PlayerError.LiveStreamError(
                    message = "HTTP error: ${httpException?.responseCode ?: "Unknown"}",
                    httpCode = httpException?.responseCode,
                    cause = exception
                )
            }

            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> {
                PlayerError.SourceError(
                    message = "Media file not found",
                    cause = exception,
                    sourceErrorType = PlayerError.SourceErrorType.FILE_NOT_FOUND
                )
            }

            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> {
                PlayerError.CodecError(
                    message = "Decoder initialization failed (Error 4001). Device may not support this format.",
                    exoPlayerErrorCode = 4001,
                    cause = exception,
                    codecErrorType = PlayerError.CodecErrorType.DECODER_INIT_FAILED
                )
            }

            PlaybackException.ERROR_CODE_DECODING_FAILED -> {
                PlayerError.CodecError(
                    message = "Failed to decode media",
                    cause = exception,
                    codecErrorType = PlayerError.CodecErrorType.DECODING_FAILED
                )
            }

            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> {
                PlayerError.CodecError(
                    message = "Media codec not supported on this device",
                    cause = exception,
                    codecErrorType = PlayerError.CodecErrorType.FORMAT_UNSUPPORTED
                )
            }

            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> {
                PlayerError.SourceError(
                    message = "Media file is corrupted or malformed",
                    cause = exception,
                    sourceErrorType = PlayerError.SourceErrorType.FILE_CORRUPTED
                )
            }

            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED -> {
                PlayerError.SourceError(
                    message = "Media container format not supported",
                    cause = exception,
                    sourceErrorType = PlayerError.SourceErrorType.UNSUPPORTED_FORMAT
                )
            }

            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> {
                PlayerError.SourceError(
                    message = "Media manifest is malformed",
                    cause = exception,
                    sourceErrorType = PlayerError.SourceErrorType.MANIFEST_MALFORMED
                )
            }

            PlaybackException.ERROR_CODE_DRM_SYSTEM_ERROR,
            PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED -> {
                PlayerError.DrmError(
                    message = "DRM license acquisition failed",
                    cause = exception,
                    drmErrorType = PlayerError.DrmErrorType.LICENSE_ACQUISITION_FAILED
                )
            }

            PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED -> {
                PlayerError.DrmError(
                    message = "DRM provisioning failed",
                    cause = exception,
                    drmErrorType = PlayerError.DrmErrorType.PROVISIONING_FAILED
                )
            }

            PlaybackException.ERROR_CODE_DRM_DEVICE_REVOKED -> {
                PlayerError.DrmError(
                    message = "Device DRM certificate has been revoked",
                    cause = exception,
                    drmErrorType = PlayerError.DrmErrorType.DEVICE_REVOKED
                )
            }

            PlaybackException.ERROR_CODE_TIMEOUT -> {
                PlayerError.TimeoutError(
                    message = "Operation timed out",
                    cause = exception
                )
            }

            else -> {
                exception.cause?.let { cause ->
                    return classifyError(cause)
                } ?: PlayerError.UnknownError(
                    message = exception.message ?: "Playback error",
                    cause = exception
                )
            }
        }
    }

    private fun classifyIOException(exception: IOException): PlayerError {
        return when (exception) {
            is UnknownHostException -> PlayerError.NetworkError(
                message = "Cannot resolve server address (DNS error)",
                cause = exception,
                retryable = true,
                networkErrorType = PlayerError.NetworkErrorType.DNS_RESOLUTION_FAILED
            )

            is SocketTimeoutException -> PlayerError.TimeoutError(
                message = "Connection timed out",
                cause = exception
            )

            is SSLException -> classifySSLException(exception)

            else -> {
                val errorType = when {
                    exception.message?.contains("ECONNREFUSED", ignoreCase = true) == true ->
                        PlayerError.NetworkErrorType.CONNECTION_REFUSED

                    exception.message?.contains("ECONNRESET", ignoreCase = true) == true ->
                        PlayerError.NetworkErrorType.CONNECTION_RESET

                    else -> PlayerError.NetworkErrorType.GENERIC
                }

                PlayerError.NetworkError(
                    message = exception.message ?: "Network error",
                    cause = exception,
                    retryable = true,
                    networkErrorType = errorType
                )
            }
        }
    }

    private fun classifySSLException(exception: SSLException): PlayerError {
        val message = when {
            exception.message?.contains("certificate", ignoreCase = true) == true ->
                "SSL certificate verification failed"

            exception.message?.contains("handshake", ignoreCase = true) == true ->
                "SSL handshake failed"

            else -> "SSL/TLS error: ${exception.message}"
        }

        return PlayerError.SSLError(
            message = message,
            certificateInfo = extractCertificateInfo(exception),
            cause = exception
        )
    }

    private fun detectNetworkErrorType(exception: PlaybackException): PlayerError.NetworkErrorType {
        val message = exception.message?.lowercase() ?: ""
        val causeMessage = exception.cause?.message?.lowercase() ?: ""

        return when {
            "unknown host" in message || "unknown host" in causeMessage ->
                PlayerError.NetworkErrorType.DNS_RESOLUTION_FAILED

            "timeout" in message || "timeout" in causeMessage ->
                PlayerError.NetworkErrorType.CONNECTION_TIMEOUT

            "refused" in message || "refused" in causeMessage ->
                PlayerError.NetworkErrorType.CONNECTION_REFUSED

            "reset" in message || "reset" in causeMessage ->
                PlayerError.NetworkErrorType.CONNECTION_RESET

            else -> PlayerError.NetworkErrorType.GENERIC
        }
    }

    private fun getNetworkErrorMessage(type: PlayerError.NetworkErrorType): String {
        return when (type) {
            PlayerError.NetworkErrorType.DNS_RESOLUTION_FAILED ->
                "Cannot resolve server address"

            PlayerError.NetworkErrorType.CONNECTION_TIMEOUT ->
                "Connection timed out"

            PlayerError.NetworkErrorType.CONNECTION_REFUSED ->
                "Server refused connection"

            PlayerError.NetworkErrorType.CONNECTION_RESET ->
                "Connection was reset"

            PlayerError.NetworkErrorType.NO_INTERNET ->
                "No internet connection available"

            else -> "Network connection failed"
        }
    }

    private fun isCorsError(httpException: HttpDataSource.InvalidResponseCodeException?): Boolean {
        if (httpException == null) return false

        val suspiciousCode = httpException.responseCode in listOf(0, 403)

        val message = httpException.message?.lowercase() ?: ""

        val hasCorsIndicators = message.contains("cors") ||
                message.contains("cross-origin") ||
                message.contains("access-control") ||
                message.contains("net::err_blocked_by_client")

        return suspiciousCode && hasCorsIndicators
    }

    private fun extractCertificateInfo(exception: SSLException): String? {
        return try {
            exception.message?.let { msg ->
                when {
                    msg.contains("Unable to find valid certification path") ->
                        "Untrusted certificate"

                    msg.contains("Certificate expired") ->
                        "Expired certificate"

                    msg.contains("Hostname") ->
                        "Hostname mismatch"

                    else -> null
                }
            }
        } catch (e: Exception) {
            logger.warning(TAG, "Failed to extract certificate info", e)
            null
        }
    }
}