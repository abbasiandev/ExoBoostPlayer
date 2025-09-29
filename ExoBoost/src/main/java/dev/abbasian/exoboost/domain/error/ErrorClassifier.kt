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
        private const val TAG = "ExoPlayerErrorClassifier"
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
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> {
                PlayerError.NetworkError(
                    message = "Network connection failed",
                    cause = exception,
                    retryable = true
                )
            }

            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> {
                val httpException = exception.cause as? HttpDataSource.InvalidResponseCodeException
                PlayerError.LiveStreamError(
                    message = "HTTP error: ${httpException?.responseCode ?: "Unknown"}",
                    httpCode = httpException?.responseCode,
                    cause = exception
                )
            }

            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> {
                PlayerError.SourceError(
                    message = "Media source not found",
                    cause = exception
                )
            }

            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED -> {
                PlayerError.CodecError(
                    message = "Failed to decode media",
                    cause = exception
                )
            }

            PlaybackException.ERROR_CODE_DRM_SYSTEM_ERROR,
            PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED -> {
                PlayerError.DrmError(
                    message = "DRM error occurred",
                    cause = exception
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
                message = "Cannot reach server: ${exception.message}",
                cause = exception,
                retryable = true,
            )

            is SocketTimeoutException -> PlayerError.TimeoutError(
                message = "Connection timed out",
                cause = exception
            )

            is SSLException -> classifySSLException(exception)

            else -> PlayerError.NetworkError(
                message = exception.message ?: "Network error",
                cause = exception,
                retryable = true
            )
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