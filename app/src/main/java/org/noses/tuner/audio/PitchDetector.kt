package org.noses.tuner.audio

import kotlin.math.sqrt

/**
 * Estimates the fundamental frequency of a mono signal using the YIN algorithm.
 * Has no Android dependencies so it can be unit tested on the JVM.
 */
class PitchDetector(
    private val sampleRate: Int,
    val windowSize: Int = 4096,
    minFrequency: Float = 60f,
    maxFrequency: Float = 1500f,
) {
    private val minLag = (sampleRate / maxFrequency).toInt()
    private val maxLag = (sampleRate / minFrequency).toInt()
    private val integrationSize = windowSize - maxLag
    private val diff = FloatArray(maxLag + 1)

    init {
        require(integrationSize > maxLag) { "windowSize too small for minFrequency" }
    }

    /** Returns the detected frequency in Hz, or null if the window is silent or unpitched. */
    fun detect(samples: FloatArray): Float? {
        require(samples.size >= windowSize)

        var energy = 0f
        for (i in 0 until integrationSize) energy += samples[i] * samples[i]
        if (sqrt(energy / integrationSize) < SILENCE_RMS) return null

        // Cumulative mean normalised difference function.
        diff[0] = 1f
        var runningSum = 0f
        for (lag in 1..maxLag) {
            var sum = 0f
            for (i in 0 until integrationSize) {
                val d = samples[i] - samples[i + lag]
                sum += d * d
            }
            runningSum += sum
            diff[lag] = if (runningSum > 0f) sum * lag / runningSum else 1f
        }

        // First dip below the threshold, followed down to its local minimum.
        var lag = minLag
        while (lag <= maxLag && diff[lag] >= THRESHOLD) lag++
        if (lag > maxLag) return null
        while (lag < maxLag && diff[lag + 1] < diff[lag]) lag++

        return sampleRate / interpolate(lag)
    }

    /** Parabolic interpolation around [lag] for sub-sample accuracy. */
    private fun interpolate(lag: Int): Float {
        if (lag < 1 || lag >= maxLag) return lag.toFloat()
        val a = diff[lag - 1]
        val b = diff[lag]
        val c = diff[lag + 1]
        val denominator = a - 2 * b + c
        return if (denominator == 0f) lag.toFloat() else lag + (a - c) / (2 * denominator)
    }

    private companion object {
        const val THRESHOLD = 0.15f
        const val SILENCE_RMS = 0.005f
    }
}
