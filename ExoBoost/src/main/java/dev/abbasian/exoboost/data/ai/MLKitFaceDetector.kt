package dev.abbasian.exoboost.data.ai

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import dev.abbasian.exoboost.util.ExoBoostLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class MLKitFaceDetector(
    private val logger: ExoBoostLogger,
) {
    companion object {
        private const val TAG = "MLKitFaceDetector"
        private const val DETECTION_TIMEOUT_MS = 5000L
        private const val MAX_RETRIES = 2
        private const val RETRY_DELAY_MS = 200L
    }

    private val options =
        FaceDetectorOptions
            .Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()

    private var detector: FaceDetector? = null
    private var isReleased = false

    private fun getDetector(): FaceDetector {
        if (detector == null || isReleased) {
            detector = FaceDetection.getClient(options)
            isReleased = false
        }
        return detector!!
    }

    suspend fun detectFaces(bitmap: Bitmap): FaceDetectionResult {
        var lastException: Exception? = null

        for (attempt in 0 until MAX_RETRIES) {
            try {
                val image = InputImage.fromBitmap(bitmap, 0)
                val faces =
                    withTimeout(DETECTION_TIMEOUT_MS) {
                        getDetector().process(image).await()
                    }

                return FaceDetectionResult(hasFaces = faces.isNotEmpty(), faceCount = faces.size)
            } catch (e: Exception) {
                lastException = e
                if (attempt < MAX_RETRIES - 1) {
                    delay(RETRY_DELAY_MS * (attempt + 1))
                }
            }
        }

        logger.error(TAG, "Face detection failed after retries", lastException)
        return FaceDetectionResult(hasFaces = false, faceCount = 0)
    }

    fun release() {
        try {
            detector?.close()
            detector = null
            isReleased = true
        } catch (e: Exception) {
            logger.warning(TAG, "Error releasing detector", e)
        }
    }
}

data class FaceDetectionResult(
    val hasFaces: Boolean,
    val faceCount: Int,
)
