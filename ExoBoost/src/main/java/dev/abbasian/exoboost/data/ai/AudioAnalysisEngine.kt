package dev.abbasian.exoboost.data.ai

import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import dev.abbasian.exoboost.domain.model.AudioScore
import dev.abbasian.exoboost.util.ExoBoostLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.math.sqrt

class AudioAnalysisEngine(
    private val logger: ExoBoostLogger,
) {
    companion object {
        private const val TAG = "AudioAnalysisEngine"
        private const val SAMPLE_INTERVAL_MS = 10000L
        private const val LOUD_THRESHOLD = 0.5f
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 100L
        private const val BATCH_SIZE = 5
        private const val BATCH_DELAY_MS = 300L
        private const val MAX_CONSECUTIVE_FAILURES = 5
    }

    suspend fun analyzeAudio(
        videoUri: Uri,
        durationMs: Long,
    ): List<AudioScore> =
        withContext(Dispatchers.IO) {
            val scores = mutableListOf<AudioScore>()
            var extractor: MediaExtractor? = null

            try {
                extractor = MediaExtractor()

                var setSourceSuccess = false
                for (retry in 0 until MAX_RETRIES) {
                    try {
                        extractor.setDataSource(videoUri.toString(), emptyMap())
                        setSourceSuccess = true
                        break
                    } catch (e: Exception) {
                        if (retry < MAX_RETRIES - 1) {
                            logger.warning(TAG, "setDataSource retry ${retry + 1}", e)
                            delay(RETRY_DELAY_MS * (retry + 1))
                        }
                    }
                }

                if (!setSourceSuccess) {
                    return@withContext emptyList()
                }

                val audioTrackIndex = findAudioTrack(extractor)
                if (audioTrackIndex == -1) {
                    logger.warning(TAG, "No audio track found")
                    return@withContext emptyList()
                }

                extractor.selectTrack(audioTrackIndex)
                val buffer = java.nio.ByteBuffer.allocate(8192)
                var currentTimeMs = 0L
                var consecutiveFailures = 0
                var sampleCount = 0

                while (currentTimeMs < durationMs && consecutiveFailures < MAX_CONSECUTIVE_FAILURES) {
                    yield()

                    try {
                        if (sampleCount > 0 && sampleCount % BATCH_SIZE == 0) {
                            delay(BATCH_DELAY_MS)
                        }

                        var seekSuccess = false
                        for (retry in 0 until MAX_RETRIES) {
                            try {
                                extractor.seekTo(
                                    currentTimeMs * 1000,
                                    MediaExtractor.SEEK_TO_CLOSEST_SYNC,
                                )
                                delay(50)
                                seekSuccess = true
                                break
                            } catch (e: Exception) {
                                if (retry < MAX_RETRIES - 1) delay(RETRY_DELAY_MS)
                            }
                        }

                        if (!seekSuccess) {
                            currentTimeMs += SAMPLE_INTERVAL_MS
                            consecutiveFailures++
                            continue
                        }

                        val sampleTime = extractor.sampleTime / 1000
                        if (sampleTime < 0) {
                            currentTimeMs += SAMPLE_INTERVAL_MS
                            continue
                        }

                        var totalAmplitude = 0.0
                        var readCount = 0

                        for (i in 0 until 3) {
                            buffer.clear()
                            val sampleSize =
                                try {
                                    extractor.readSampleData(buffer, 0)
                                } catch (e: Exception) {
                                    -1
                                }

                            if (sampleSize > 0) {
                                val amplitude = calculateRMS(buffer, sampleSize)
                                if (amplitude > 0) {
                                    totalAmplitude += amplitude
                                    readCount++
                                }
                            }

                            if (!extractor.advance()) break
                            delay(10)
                        }

                        if (readCount > 0) {
                            val avgAmplitude = (totalAmplitude / readCount).toFloat()
                            val normalizedVolume = (avgAmplitude / 32768.0f).coerceIn(0f, 1f)

                            scores.add(
                                AudioScore(
                                    timestampMs = sampleTime,
                                    volumeLevel = normalizedVolume,
                                    isLoud = normalizedVolume > LOUD_THRESHOLD,
                                ),
                            )
                            consecutiveFailures = 0
                            sampleCount++
                        }
                    } catch (e: Exception) {
                        consecutiveFailures++
                        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) break
                        delay(RETRY_DELAY_MS * consecutiveFailures)
                    }

                    currentTimeMs += SAMPLE_INTERVAL_MS
                }

                logger.info(TAG, "Audio analysis complete: ${scores.size} samples")
                scores
            } catch (e: Exception) {
                logger.error(TAG, "Audio analysis failed", e)
                emptyList()
            } finally {
                try {
                    extractor?.release()
                } catch (e: Exception) {
                    logger.warning(TAG, "Error releasing extractor", e)
                }
            }
        }

    private fun findAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) return i
        }
        return -1
    }

    private fun calculateRMS(
        buffer: java.nio.ByteBuffer,
        size: Int,
    ): Double {
        if (size < 2) return 0.0
        try {
            buffer.position(0)
            buffer.limit(size)
            var sum = 0.0
            var sampleCount = 0
            val maxSamples = minOf(size / 2, 1000)

            while (buffer.remaining() >= 2 && sampleCount < maxSamples) {
                val sample = buffer.short.toDouble()
                sum += sample * sample
                sampleCount++
            }
            return if (sampleCount > 0) sqrt(sum / sampleCount) else 0.0
        } catch (e: Exception) {
            return 0.0
        }
    }
}
