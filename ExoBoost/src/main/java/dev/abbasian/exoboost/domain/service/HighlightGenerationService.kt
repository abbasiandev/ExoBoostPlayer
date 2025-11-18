package dev.abbasian.exoboost.domain.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dev.abbasian.exoboost.R
import dev.abbasian.exoboost.domain.model.HighlightConfig
import dev.abbasian.exoboost.domain.model.VideoHighlights
import dev.abbasian.exoboost.domain.usecase.GenerateVideoHighlightsUseCase
import dev.abbasian.exoboost.presentation.state.HighlightsState
import dev.abbasian.exoboost.util.ExoBoostLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class HighlightGenerationService : Service() {
    companion object {
        private const val TAG = "HighlightGenerationService"
        private const val NOTIFICATION_ID = 9001
        private const val CHANNEL_ID = "highlight_generation_channel"
        private const val CHANNEL_NAME = "Highlight Generation"

        const val ACTION_START = "dev.abbasian.exoboost.START_HIGHLIGHT_GENERATION"
        const val ACTION_STOP = "dev.abbasian.exoboost.STOP_HIGHLIGHT_GENERATION"
        const val ACTION_CANCEL = "dev.abbasian.exoboost.CANCEL_HIGHLIGHT_GENERATION"

        const val EXTRA_VIDEO_URL = "video_url"
        const val EXTRA_CONFIG = "highlight_config"

        fun startService(
            context: Context,
            videoUrl: String,
            config: HighlightConfig,
        ) {
            val intent = Intent(context, HighlightGenerationService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_VIDEO_URL, videoUrl)
                putExtra(EXTRA_CONFIG, config) // Use Parcelable directly
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent =
                Intent(context, HighlightGenerationService::class.java).apply {
                    action = ACTION_STOP
                }
            context.stopService(intent)
        }
    }

    private val binder = LocalBinder()
    private val logger: ExoBoostLogger by inject()
    private val generateHighlightsUseCase: GenerateVideoHighlightsUseCase by inject()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var currentJob: Job? = null

    private val _highlightsState = MutableStateFlow<HighlightsState>(HighlightsState.Idle)
    val highlightsState: StateFlow<HighlightsState> = _highlightsState.asStateFlow()

    private lateinit var notificationManager: NotificationManager
    private var isGenerating = false

    inner class LocalBinder : Binder() {
        fun getService(): HighlightGenerationService = this@HighlightGenerationService
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        logger.info(TAG, "Service created")
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_START -> {
                val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL)
                val config = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_CONFIG, HighlightConfig::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_CONFIG)
                } ?: HighlightConfig()

                if (videoUrl != null) {
                    startHighlightGeneration(videoUrl, config)
                }
            }

            ACTION_CANCEL -> {
                cancelHighlightGeneration()
            }

            ACTION_STOP -> {
                stopHighlightGeneration()
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Shows progress of video highlight generation"
                    setShowBadge(false)
                }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(progress: String): Notification {
        val cancelIntent =
            Intent(this, HighlightGenerationService::class.java).apply {
                action = ACTION_CANCEL
            }
        val cancelPendingIntent =
            PendingIntent.getService(
                this,
                0,
                cancelIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        return NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setContentTitle("Generating Video Highlights")
            .setContentText(progress)
            .setSmallIcon(R.drawable.ic_highlight)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                R.drawable.ic_close,
                "Cancel",
                cancelPendingIntent,
            ).build()
    }

    private fun updateNotification(progress: String) {
        val notification = createNotification(progress)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun startHighlightGeneration(
        videoUrl: String,
        config: HighlightConfig,
    ) {
        if (isGenerating) {
            logger.warning(TAG, "Highlight generation already in progress")
            return
        }

        isGenerating = true
        startForeground(NOTIFICATION_ID, createNotification("Starting analysis..."))

        currentJob =
            serviceScope.launch {
                try {
                    logger.info(TAG, "Starting highlight generation for: $videoUrl")

                    _highlightsState.value = HighlightsState.Analyzing("Analyzing video...")
                    updateNotification("Analyzing video...")

                    val uri = Uri.parse(videoUrl)
                    val result = generateHighlightsUseCase.execute(uri, config)

                    result.onSuccess { highlights ->
                        logger.info(
                            TAG,
                            "Highlights generated successfully: ${highlights.highlights.size} segments",
                        )

                        _highlightsState.value = HighlightsState.Success(highlights)

                        // Show completion notification
                        showCompletionNotification(highlights)

                        // Stop service after a delay
                        kotlinx.coroutines.delay(3000)
                        stopSelf()
                    }

                    result.onFailure { error ->
                        logger.error(TAG, "Highlight generation failed", error)
                        val errorMessage = error.message ?: "Failed to generate highlights"
                        _highlightsState.value = HighlightsState.Error(errorMessage)

                        showErrorNotification(errorMessage)

                        kotlinx.coroutines.delay(3000)
                        stopSelf()
                    }
                } catch (e: Exception) {
                    logger.error(TAG, "Unexpected error during highlight generation", e)
                    _highlightsState.value =
                        HighlightsState.Error(
                            e.message ?: "An unexpected error occurred",
                        )

                    showErrorNotification(e.message ?: "An unexpected error occurred")

                    kotlinx.coroutines.delay(3000)
                    stopSelf()
                } finally {
                    isGenerating = false
                }
            }
    }

    private fun cancelHighlightGeneration() {
        logger.info(TAG, "Cancelling highlight generation")
        currentJob?.cancel()
        _highlightsState.value = HighlightsState.Idle
        isGenerating = false
        stopSelf()
    }

    private fun stopHighlightGeneration() {
        logger.info(TAG, "Stopping highlight generation service")
        currentJob?.cancel()
        isGenerating = false
        stopSelf()
    }

    private fun showCompletionNotification(highlights: VideoHighlights) {
        val notification =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setContentTitle("Highlights Ready!")
                .setContentText("Generated ${highlights.highlights.size} highlight segments")
                .setSmallIcon(R.drawable.ic_highlight)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun showErrorNotification(errorMessage: String) {
        val notification =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setContentTitle("Highlight Generation Failed")
                .setContentText(errorMessage)
                .setSmallIcon(R.drawable.ic_error)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

        notificationManager.notify(NOTIFICATION_ID + 2, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        currentJob?.cancel()
        serviceScope.cancel()
        isGenerating = false
        logger.info(TAG, "Service destroyed")
    }
}