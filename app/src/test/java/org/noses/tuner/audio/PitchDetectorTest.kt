package org.noses.tuner.audio

import kotlin.math.PI
import kotlin.math.log2
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PitchDetectorTest {
    private val sampleRate = 44100
    private val detector = PitchDetector(sampleRate)

    private fun tone(frequency: Double, harmonics: Int = 1) = FloatArray(detector.windowSize) { i ->
        var sample = 0.0
        for (h in 1..harmonics) sample += sin(2 * PI * frequency * h * i / sampleRate) / h
        (0.3 * sample).toFloat()
    }

    /** Asserts [samples] are detected as [frequency] to within a cent, far inside the in-tune band. */
    private fun assertDetects(frequency: Double, samples: FloatArray) {
        val detected = detector.detect(samples)
        assertNotNull("no pitch for $frequency", detected)
        assertEquals("cents off $frequency", 0.0, 1200 * log2(detected!! / frequency), 1.0)
    }

    @Test
    fun detectsGuitarOpenStrings() {
        for (frequency in listOf(82.41, 110.0, 146.83, 196.0, 246.94, 329.63)) {
            assertDetects(frequency, tone(frequency, harmonics = 4))
        }
    }

    @Test
    fun detectsPureA440() {
        assertDetects(440.0, tone(440.0))
    }

    @Test
    fun detectsPitchesNearTheEdgesOfTheRange() {
        assertDetects(65.0, tone(65.0))
        assertDetects(1400.0, tone(1400.0))
    }

    @Test
    fun pitchesBelowTheRangeHaveNoPitch() {
        for (frequency in listOf(40.0, 50.0, 55.0, 59.0)) {
            assertNull("pitch for $frequency", detector.detect(tone(frequency)))
        }
    }

    @Test
    fun pitchesAboveTheRangeHaveNoPitch() {
        for (frequency in listOf(1600.0, 2000.0, 3000.0)) {
            assertNull("pitch for $frequency", detector.detect(tone(frequency)))
        }
    }

    @Test
    fun silenceHasNoPitch() {
        assertNull(detector.detect(FloatArray(detector.windowSize)))
    }
}
