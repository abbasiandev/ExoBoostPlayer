package dev.abbasian.exoboost.domain.error

import android.content.Context
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import dev.abbasian.exoboost.R
import dev.abbasian.exoboost.domain.model.PlayerError
import dev.abbasian.exoboost.util.ExoBoostLogger
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

@UnstableApi
class ErrorClassifier(
    private val context: Context,
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
                message = exception.message ?: context.getString(R.string.error_msg_unknown),
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
                    message = context.getString(R.string.error_msg_network_timeout),
                    cause = exception,
                    retryable = true,
                    networkErrorType = PlayerError.NetworkErrorType.CONNECTION_TIMEOUT
                )
            }

            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> {
                val httpException = exception.cause as? HttpDataSource.InvalidResponseCodeException

                if (isCorsError(httpException)) {
                    return PlayerError.SourceError(
                        message = context.getString(R.string.error_msg_cors),
                        sourceUrl = httpException?.dataSpec?.uri?.toString(),
                        cause = exception,
                        sourceErrorType = PlayerError.SourceErrorType.CORS_ERROR
                    )
                }

                PlayerError.LiveStreamError(
                    message = context.getString(
                        R.string.error_msg_http,
                        httpException?.responseCode?.toString() ?: "Unknown"
                    ),
                    httpCode = httpException?.responseCode,
                    cause = exception
                )
            }

            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> {
                PlayerError.SourceError(
                    message = context.getString(R.string.error_msg_file_not_found),
                    cause = exception,
                    sourceErrorType = PlayerError.SourceErrorType.FILE_NOT_FOUND
                )
            }

            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> {
                PlayerError.CodecError(
                    message = context.getString(R.string.error_msg_decoder_init),
                    exoPlayerErrorCode = 4001,
                    cause = exception,
                    codecErrorType = PlayerError.CodecErrorType.DECODER_INIT_FAILED
                )
            }

            PlaybackException.ERROR_CODE_DECODING_FAILED -> {
                PlayerError.CodecError(
                    message = context.getString(R.string.error_msg_decoding_failed),
                    cause = exception,
                    codecErrorType = PlayerError.CodecErrorType.DECODING_FAILED
                )
            }

            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> {
                PlayerError.CodecError(
                    message = context.getString(R.string.error_msg_codec_unsupported),
                    cause = exception,
                    codecErrorType = PlayerError.CodecErrorType.FORMAT_UNSUPPORTED
                )
            }

            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> {
                PlayerError.SourceError(
                    message = context.getString(R.string.error_msg_file_corrupted),
                    cause = exception,
                    sourceErrorType = PlayerError.SourceErrorType.FILE_CORRUPTED
                )
            }

            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED -> {
                PlayerError.SourceError(
                    message = context.getString(R.string.error_msg_container_unsupported),
                    cause = exception,
                    sourceErrorType = PlayerError.SourceErrorType.UNSUPPORTED_FORMAT
                )
            }

            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> {
                PlayerError.SourceError(
                    message = context.getString(R.string.error_msg_manifest_malformed),
                    cause = exception,
                    sourceErrorType = PlayerError.SourceErrorType.MANIFEST_MALFORMED
                )
            }

            PlaybackException.ERROR_CODE_DRM_SYSTEM_ERROR,
            PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED -> {
                PlayerError.DrmError(
                    message = context.getString(R.string.error_msg_drm_license),
                    cause = exception,
                    drmErrorType = PlayerError.DrmErrorType.LICENSE_ACQUISITION_FAILED
                )
            }

            PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED -> {
                PlayerError.DrmError(
                    message = context.getString(R.string.error_msg_drm_provisioning),
                    cause = exception,
                    drmErrorType = PlayerError.DrmErrorType.PROVISIONING_FAILED
                )
            }

            PlaybackException.ERROR_CODE_DRM_DEVICE_REVOKED -> {
                PlayerError.DrmError(
                    message = context.getString(R.string.error_msg_drm_device_revoked),
                    cause = exception,
                    drmErrorType = PlayerError.DrmErrorType.DEVICE_REVOKED
                )
            }

            PlaybackException.ERROR_CODE_TIMEOUT -> {
                PlayerError.TimeoutError(
                    message = context.getString(R.string.error_msg_timeout),
                    cause = exception
                )
            }

            else -> {
                exception.cause?.let { cause ->
                    return classifyError(cause)
                } ?: PlayerError.UnknownError(
                    message = exception.message ?: context.getString(R.string.error_msg_playback),
                    cause = exception
                )
            }
        }
    }

    private fun classifyIOException(exception: IOException): PlayerError {
        return when (exception) {
            is UnknownHostException -> PlayerError.NetworkError(
                message = context.getString(R.string.error_msg_dns),
                cause = exception,
                retryable = true,
                networkErrorType = PlayerError.NetworkErrorType.DNS_RESOLUTION_FAILED
            )

            is SocketTimeoutException -> PlayerError.TimeoutError(
                message = context.getString(R.string.error_msg_connection_timeout),
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
                    message = exception.message ?: context.getString(R.string.error_msg_network_generic),
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
                context.getString(R.string.error_msg_ssl_certificate)

            exception.message?.contains("handshake", ignoreCase = true) == true ->
                context.getString(R.string.error_msg_ssl_handshake)

            else -> context.getString(R.string.error_msg_ssl_generic, exception.message ?: "")
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
                context.getString(R.string.error_msg_dns_failed)

            PlayerError.NetworkErrorType.CONNECTION_TIMEOUT ->
                context.getString(R.string.error_msg_connection_timeout)

            PlayerError.NetworkErrorType.CONNECTION_REFUSED ->
                context.getString(R.string.error_msg_connection_refused)

            PlayerError.NetworkErrorType.CONNECTION_RESET ->
                context.getString(R.string.error_msg_connection_reset)

            PlayerError.NetworkErrorType.NO_INTERNET ->
                context.getString(R.string.error_msg_no_internet)

            else -> context.getString(R.string.error_msg_network_failed)
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
                        context.getString(R.string.cert_info_untrusted)

                    msg.contains("Certificate expired") ->
                        context.getString(R.string.cert_info_expired)

                    msg.contains("Hostname") ->
                        context.getString(R.string.cert_info_hostname_mismatch)

                    else -> null
                }
            }
        } catch (e: Exception) {
            logger.warning(TAG, "Failed to extract certificate info", e)
            null
        }
    }
}