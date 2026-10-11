package org.noses.tuner.audio

import kotlin.math.PI
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

    @Test
    fun detectsGuitarOpenStrings() {
        for (frequency in listOf(82.41, 110.0, 146.83, 196.0, 246.94, 329.63)) {
            val detected = detector.detect(tone(frequency, harmonics = 4))
            assertNotNull("no pitch for $frequency", detected)
            assertEquals(frequency, detected!!.toDouble(), 0.3)
        }
    }

    @Test
    fun detectsPureA440() {
        assertEquals(440.0, detector.detect(tone(440.0))!!.toDouble(), 0.5)
    }

    @Test
    fun silenceHasNoPitch() {
        assertNull(detector.detect(FloatArray(detector.windowSize)))
    }
}
