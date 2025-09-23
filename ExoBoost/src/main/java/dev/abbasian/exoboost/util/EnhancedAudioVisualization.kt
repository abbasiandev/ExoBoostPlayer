package dev.abbasian.exoboost.util

import android.media.audiofx.Visualizer
import android.util.Log
import dev.abbasian.exoboost.domain.model.VisualizationType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

internal class EnhancedAudioVisualization {
    private var visualizer: Visualizer? = null
    private var visualizationData = FloatArray(64) { 0f }
    private var bassIntensity = 0f
    private var midIntensity = 0f
    private var trebleIntensity = 0f
    private var hasData = false

    // smooth transitions
    private var previousData = FloatArray(64) { 0f }
    private var peakValues = FloatArray(64) { 0f }
    private val random = Random.Default

    private fun setupRealAudioVisualizer(audioSessionId: Int): Boolean {
        return try {
            visualizer?.release()
            if (audioSessionId != 0) {
                visualizer = Visualizer(audioSessionId).apply {
                    captureSize = Visualizer.getCaptureSizeRange()[1]
                    setDataCaptureListener(
                        object : Visualizer.OnDataCaptureListener {
                            override fun onWaveFormDataCapture(
                                visualizer: Visualizer,
                                waveform: ByteArray,
                                samplingRate: Int
                            ) {
                                processRealWaveformData(waveform)
                            }

                            override fun onFftDataCapture(
                                visualizer: Visualizer,
                                fft: ByteArray,
                                samplingRate: Int
                            ) {
                                processRealFFTData(fft)
                            }
                        },
                        Visualizer.getMaxCaptureRate() / 2,
                        true,
                        true
                    )
                    enabled = true
                }
            }
            true
        } catch (e: Exception) {
            Log.e("AudioVisualizer", "Failed to setup real audio visualizer", e)
            false
        }
    }

    private fun processRealWaveformData(waveform: ByteArray) {
        val dataSize = visualizationData.size
        for (i in 0 until dataSize) {
            val waveIndex = (i * waveform.size / dataSize).coerceIn(0, waveform.size - 1)
            val amplitude = abs(waveform[waveIndex].toFloat() / 128f)
            visualizationData[i] = amplitude.coerceIn(0f, 1f)
        }

        bassIntensity = calculateBandIntensity(waveform, 0, waveform.size / 4)
        midIntensity = calculateBandIntensity(waveform, waveform.size / 4, 3 * waveform.size / 4)
        trebleIntensity = calculateBandIntensity(waveform, 3 * waveform.size / 4, waveform.size)
    }

    private fun calculateBandIntensity(data: ByteArray, start: Int, end: Int): Float {
        var sum = 0f
        var count = 0
        for (i in start until end.coerceAtMost(data.size)) {
            sum += abs(data[i].toFloat())
            count++
        }
        return if (count > 0) (sum / count / 128f).coerceIn(0f, 1f) else 0f
    }

    private fun processRealFFTData(fft: ByteArray) {
        val dataSize = visualizationData.size
        val fftSize = fft.size / 2

        for (i in 0 until dataSize) {
            val fftIndex = (i * fftSize / dataSize).coerceIn(0, fftSize - 1)
            val realIndex = fftIndex * 2

            if (realIndex < fft.size) {
                val magnitude = (fft[realIndex].toUByte().toFloat() / 255f)
                visualizationData[i] = magnitude.coerceIn(0f, 1f)
            }
        }

        // band intensities from FFT data
        val bassEnd = fftSize / 8      // Low frequencies
        val midEnd = fftSize / 2       // Mid frequencies
        val trebleEnd = fftSize        // High frequencies

        bassIntensity = calculateFFTBandIntensity(fft, 0, bassEnd)
        midIntensity = calculateFFTBandIntensity(fft, bassEnd, midEnd)
        trebleIntensity = calculateFFTBandIntensity(fft, midEnd, trebleEnd)
    }

    private fun calculateFFTBandIntensity(fft: ByteArray, startBin: Int, endBin: Int): Float {
        var sum = 0f
        var count = 0

        for (i in startBin until endBin.coerceAtMost(fft.size / 2)) {
            val magnitudeIndex = i * 2
            if (magnitudeIndex < fft.size) {
                sum += fft[magnitudeIndex].toUByte().toFloat()
                count++
            }
        }

        return if (count > 0) (sum / count / 255f).coerceIn(0f, 1f) else 0f
    }

    fun updateVisualization(
        isPlaying: Boolean,
        audioSessionId: Int,
        visualizationType: VisualizationType,
        sensitivity: Float,
        smoothingFactor: Float
    ) {
        if (!isPlaying) {
            clearVisualization()
            return
        }

        val hasRealAudio = setupRealAudioVisualizer(audioSessionId)

        if (!hasRealAudio) {
            val dataSize = getDataSize(visualizationType)
            generateReactiveVisualizationData(
                dataSize = dataSize,
                visualizationType = visualizationType,
                sensitivity = sensitivity,
                smoothingFactor = smoothingFactor
            )
        }

        hasData = true
    }

    private fun generateReactiveVisualizationData(
        dataSize: Int,
        visualizationType: VisualizationType,
        sensitivity: Float,
        smoothingFactor: Float
    ) {
        val currentTime = System.currentTimeMillis()
        val timeSeconds = (currentTime / 2000.0f)

        val energyLevel = 0.5f + kotlin.math.sin(timeSeconds * 0.8f) * 0.3f

        bassIntensity = generateBandIntensity(timeSeconds, 1.2f, energyLevel) * sensitivity
        midIntensity = generateBandIntensity(timeSeconds, 2.1f, energyLevel) * sensitivity
        trebleIntensity = generateBandIntensity(timeSeconds, 3.8f, energyLevel) * sensitivity

        if (visualizationData.size != dataSize) {
            visualizationData = FloatArray(dataSize) { 0f }
            previousData = FloatArray(dataSize) { 0f }
            peakValues = FloatArray(dataSize) { 0f }
        }

        for (i in 0 until dataSize) {
            val frequency = i.toFloat() / dataSize
            val newValue = generateFrequencyResponse(
                frequency = frequency,
                timeSeconds = timeSeconds,
                bassIntensity = bassIntensity,
                midIntensity = midIntensity,
                trebleIntensity = trebleIntensity,
                visualizationType = visualizationType
            )

            peakValues[i] = max(peakValues[i] * 0.95f, newValue)
            val smoothedValue =
                previousData[i] * (1f - smoothingFactor) + newValue * smoothingFactor

            visualizationData[i] = min(smoothedValue, peakValues[i]).coerceIn(0f, 1f)
            previousData[i] = visualizationData[i]
        }
    }

    private fun generateBandIntensity(
        timeSeconds: Float,
        frequency: Float,
        energyLevel: Float
    ): Float {
        val beatTime = timeSeconds * 2.0f

        val kickPattern = when {
            frequency < 1.5f -> {
                val beat = kotlin.math.floor(beatTime) % 4.0f
                when (beat.toInt()) {
                    0 -> 1.0f // strong beat
                    2 -> 0.7f // medium beat
                    else -> 0.2f // weak beats
                }
            }

            else -> 0.0f
        }

        val snarePattern = when {
            frequency in 1.5f..2.5f -> {
                val beat = kotlin.math.floor(beatTime) % 4.0f
                if (beat.toInt() == 1 || beat.toInt() == 3) 0.8f else 0.1f
            }

            else -> 0.0f
        }

        val hihatPattern = when {
            frequency > 2.5f -> {
                val subBeat = kotlin.math.floor(beatTime * 2.0f) % 8.0f
                if (subBeat.toInt() % 2 == 0) 0.6f else 0.3f
            }

            else -> 0.0f
        }

        val phraseLength = 32.0f
        val phrasePosition = (beatTime % phraseLength) / phraseLength
        val phraseIntensity = when {
            phrasePosition < 0.25f -> 0.6f
            phrasePosition < 0.75f -> 1.0f
            else -> 0.4f
        }

        val rhythmBase = (kickPattern + snarePattern + hihatPattern) * phraseIntensity
        val musicalVariation = kotlin.math.sin(beatTime * 0.5f + frequency) * 0.15f
        val dynamicRange = kotlin.math.sin(beatTime * 0.125f) * 0.2f // Slow dynamics

        val finalIntensity = (rhythmBase + musicalVariation + dynamicRange) * energyLevel

        return abs(finalIntensity).coerceIn(0f, 1f)
    }

    private fun generateFrequencyResponse(
        frequency: Float,
        timeSeconds: Float,
        bassIntensity: Float,
        midIntensity: Float,
        trebleIntensity: Float,
        visualizationType: VisualizationType
    ): Float {
        val randomFactor = random.nextFloat() * 0.2f

        val response = when {
            frequency < 0.3f -> {
                // Bass frequencies
                bassIntensity * kotlin.math.sin(timeSeconds * 4.0f + frequency * 10.0f) *
                        (1f - frequency * 2f) + randomFactor
            }

            frequency < 0.7f -> {
                // Mid frequencies
                val midFactor = 1f - abs(frequency - 0.5f) * 2f
                midIntensity * midFactor * kotlin.math.cos(timeSeconds * 6.0f + frequency * 15.0f) +
                        randomFactor
            }

            else -> {
                // Treble frequencies
                trebleIntensity * kotlin.math.sin(timeSeconds * 8.0f + frequency * 20.0f) *
                        (frequency * 1.5f) + randomFactor
            }
        }

        // Add visualization-specific enhancements
        val typeMultiplier = when (visualizationType) {
            VisualizationType.SPECTRUM -> 1.0f
            VisualizationType.WAVEFORM -> 0.8f + kotlin.math.sin(timeSeconds + frequency * 6.28f) * 0.2f
            VisualizationType.CIRCULAR -> 0.9f + kotlin.math.cos(timeSeconds * 2f) * 0.1f
            VisualizationType.BARS -> 0.7f + random.nextFloat() * 0.3f
            VisualizationType.PARTICLE_SYSTEM -> 0.8f + kotlin.math.sin(timeSeconds * 3f + frequency * 12f) * 0.2f
        }

        return abs(response * typeMultiplier).coerceIn(0f, 1f)
    }

    private fun getDataSize(visualizationType: VisualizationType): Int {
        return when (visualizationType) {
            VisualizationType.SPECTRUM -> 48
            VisualizationType.WAVEFORM -> 64
            VisualizationType.CIRCULAR -> 40
            VisualizationType.BARS -> 32
            VisualizationType.PARTICLE_SYSTEM -> 24
        }
    }

    fun clearVisualization() {
        visualizationData.fill(0f)
        previousData.fill(0f)
        peakValues.fill(0f)
        bassIntensity = 0f
        midIntensity = 0f
        trebleIntensity = 0f
        hasData = false
    }

    fun hasData(): Boolean = hasData
    fun getVisualizationData(): FloatArray = visualizationData
    fun getBassIntensity(): Float = bassIntensity
    fun getMidIntensity(): Float = midIntensity
    fun getTrebleIntensity(): Float = trebleIntensity

    fun release() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
            visualizer = null
        } catch (e: Exception) {
            Log.e("AudioVisualizer", "Error releasing visualizer", e)
        }
        clearVisualization()
    }
}