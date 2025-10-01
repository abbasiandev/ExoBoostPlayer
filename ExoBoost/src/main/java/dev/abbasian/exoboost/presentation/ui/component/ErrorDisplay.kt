package dev.abbasian.exoboost.presentation.ui.component

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.abbasian.exoboost.domain.model.PlayerError
import dev.abbasian.exoboost.R

@Composable
fun ErrorDisplay(
    error: PlayerError,
    onRetry: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier.padding(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = getErrorIcon(error),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = getErrorTitle(context, error),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = error.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isRetryableError(error)) {
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(context.getString(R.string.action_retry))
                    }
                }

                onDismiss?.let { dismiss ->
                    OutlinedButton(onClick = dismiss) {
                        Text(context.getString(R.string.action_back))
                    }
                }
            }

            // help text for specific errors
            getErrorHelpText(context, error)?.let { helpText ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = helpText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun getErrorIcon(error: PlayerError): ImageVector {
    return when (error) {
        is PlayerError.NetworkError -> Icons.Filled.SignalWifiOff
        is PlayerError.SSLError -> Icons.Filled.Security
        is PlayerError.CodecError -> Icons.Filled.VideoSettings
        is PlayerError.SourceError -> Icons.Filled.BrokenImage
        is PlayerError.LiveStreamError -> Icons.Filled.LiveTv
        is PlayerError.DrmError -> Icons.Filled.Lock
        is PlayerError.TimeoutError -> Icons.Filled.Schedule
        is PlayerError.UnknownError -> Icons.Filled.Error
    }
}

private fun getErrorTitle(context: Context, error: PlayerError): String {
    return when (error) {
        is PlayerError.NetworkError -> context.getString(R.string.error_network)
        is PlayerError.SSLError -> context.getString(R.string.error_security)
        is PlayerError.CodecError -> context.getString(R.string.error_decoding)
        is PlayerError.SourceError -> context.getString(R.string.error_source)
        is PlayerError.LiveStreamError -> context.getString(R.string.error_live)
        is PlayerError.DrmError -> context.getString(R.string.error_drm)
        is PlayerError.TimeoutError -> context.getString(R.string.error_timeout)
        is PlayerError.UnknownError -> context.getString(R.string.error_unknown)
    }
}

private fun isRetryableError(error: PlayerError): Boolean {
    return when (error) {
        is PlayerError.NetworkError -> error.isRetryable
        is PlayerError.LiveStreamError -> error.httpCode in listOf(403, 500, 502, 503, 504)
        is PlayerError.SourceError -> true
        is PlayerError.UnknownError -> true
        else -> false
    }
}

private fun getErrorHelpText(context: Context, error: PlayerError): String? {
    return when (error) {
        is PlayerError.NetworkError -> when (error.networkErrorType) {
            PlayerError.NetworkErrorType.DNS_RESOLUTION_FAILED ->
                "Check your DNS settings or try using a different network"
            PlayerError.NetworkErrorType.CONNECTION_REFUSED ->
                "Server is not accepting connections. Try again later"
            PlayerError.NetworkErrorType.NO_INTERNET ->
                context.getString(R.string.help_check_network)
            else -> context.getString(R.string.help_check_network)
        }

        is PlayerError.CodecError -> when (error.codecErrorType) {
            PlayerError.CodecErrorType.DECODER_NOT_AVAILABLE,
            PlayerError.CodecErrorType.DECODER_INIT_FAILED ->
                "Your device doesn't support this video format. Try a different quality or format"
            PlayerError.CodecErrorType.FORMAT_UNSUPPORTED ->
                "This video format is not supported on your device"
            PlayerError.CodecErrorType.DECODING_FAILED,
            PlayerError.CodecErrorType.GENERIC ->
                "Try updating your device or using a different video format"
        }

        is PlayerError.SourceError -> when (error.sourceErrorType) {
            PlayerError.SourceErrorType.FILE_CORRUPTED ->
                "The video file appears to be corrupted"
            PlayerError.SourceErrorType.CORS_ERROR ->
                "Server blocked access due to security policy (CORS)"
            PlayerError.SourceErrorType.FILE_NOT_FOUND ->
                context.getString(R.string.help_not_found)
            else -> null
        }

        is PlayerError.SSLError ->
            context.getString(R.string.help_ssl_invalid)

        is PlayerError.DrmError -> when (error.drmErrorType) {
            PlayerError.DrmErrorType.LICENSE_ACQUISITION_FAILED ->
                "Failed to acquire DRM license. Check your subscription or authentication."

            PlayerError.DrmErrorType.PROVISIONING_FAILED ->
                "DRM provisioning failed. Your device may not support this content protection."

            PlayerError.DrmErrorType.DEVICE_REVOKED ->
                "Your device's DRM certificate has been revoked and cannot play protected content."

            PlayerError.DrmErrorType.LICENSE_EXPIRED ->
                "DRM license has expired. Please renew your subscription or re-authenticate."

            PlayerError.DrmErrorType.GENERIC ->
                context.getString(R.string.help_drm)
        }

        is PlayerError.TimeoutError ->
            "Server is taking too long to respond. Please try again"

        is PlayerError.LiveStreamError -> when (error.httpCode) {
            403 -> context.getString(R.string.help_access_forbidden)
            404 -> context.getString(R.string.help_not_found)
            500, 502, 503, 504 -> "Server is temporarily unavailable"
            else -> null
        }

        is PlayerError.UnknownError -> null
    }
}