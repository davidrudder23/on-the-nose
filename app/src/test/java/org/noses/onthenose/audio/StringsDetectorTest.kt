package org.noses.onthenose.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class StringsDetectorTest {
    private val sampleRate = 44100

    private fun detector(tuning: Tuning) = StringsDetector(sampleRate, tuning.strings.map { midiFrequency(it) })

    /**
     * A plucked string: overtones with uneven, falling levels and random phases, decaying over the
     * window. [level] scales the whole string.
     */
    private fun pluck(frequency: Double, random: Random, level: Double = 1.0): (Int) -> Double {
        val harmonics = (1..10).filter { frequency * it < sampleRate / 2 }
        val amplitudes = harmonics.map { h -> level * (0.4 + 0.6 * random.nextDouble()) / h }
        val phases = harmonics.map { random.nextDouble() * 2 * PI }
        return { i ->
            val t = i.toDouble() / sampleRate
            var sample = 0.0
            for (n in harmonics.indices) {
                sample += amplitudes[n] * sin(2 * PI * frequency * harmonics[n] * t + phases[n])
            }
            sample * exp(-t * 1.5)
        }
    }

    /** Strums [tuning] with each string off by the given cents, null leaving that string silent. */
    private fun strum(
        tuning: Tuning,
        cents: List<Double?>,
        seed: Int = 1,
        noise: Double = 0.002,
        size: Int = 16384,
    ): FloatArray {
        val random = Random(seed)
        val strings = tuning.strings.zip(cents).mapNotNull { (midi, offset) ->
            offset?.let { pluck(midiFrequency(midi) * 2.0.pow(it / 1200), random) }
        }
        return FloatArray(size) { i ->
            val sample = strings.sumOf { it(i) } * 0.08 + noise * (random.nextDouble() * 2 - 1)
            sample.toFloat()
        }
    }

    private fun centsOff(detected: Float, midi: Int) = 1200 * log2(detected / midiFrequency(midi))

    private fun assertReads(tuning: Tuning, cents: List<Double?>, readings: List<Float?>, tolerance: Double) {
        tuning.strings.indices.forEach { i ->
            val expected = cents[i]
            if (expected == null) {
                assertNull("${tuning.name} string ${6 - i} should be silent", readings[i])
            } else {
                val reading = readings[i]
                assertNotNull("${tuning.name} string ${6 - i} not heard", reading)
                assertEquals(
                    "${tuning.name} string ${6 - i}",
                    expected, centsOff(reading!!, tuning.strings[i]).toDouble(), tolerance,
                )
            }
        }
    }

    private val tunings = Tuning.entries.filter { it.hasStrings }

    /** Tunings with a single string per note, apart from the two Es (or Ds) two octaves apart. */
    private val closedTunings = listOf(Tuning.STANDARD, Tuning.DROP_D, Tuning.HALF_STEP_DOWN)

    @Test
    fun readsAnInTuneStrum() {
        val inTune = List(6) { 0.0 }
        for (tuning in tunings) {
            // Open G's B is 14 cents sharp of the low D's fifth overtone (the gap between an
            // equal-tempered and a pure major third), and that nearby peak nudges it slightly.
            assertReads(tuning, inTune, detector(tuning).detect(strum(tuning, inTune)), tolerance = 3.0)
        }
    }

    @Test
    fun readsEachStringsOwnOffsetInAStrum() {
        val offsets = listOf(-30.0, 12.0, -7.0, 25.0, -18.0, 40.0)
        for (tuning in closedTunings) {
            for (seed in 1..5) {
                val readings = detector(tuning).detect(strum(tuning, offsets, seed))
                assertReads(tuning, offsets, readings, tolerance = 5.0)
            }
        }
    }

    @Test
    fun readsSinglePlucksInEveryTuning() {
        // Open tunings repeat notes an octave apart, and an octave string shares every overtone of
        // the one below, so in a strum their readings pull toward each other by several cents
        // unless both are in tune. One string at a time reads cleanly in any tuning. Other
        // gauges may pick up the plucked string's overtones, so only the plucked one is checked.
        for (tuning in tunings) {
            for (string in 0 until 6) {
                val offsets = List(6) { if (it == string) 17.0 else null }
                val reading = detector(tuning).detect(strum(tuning, offsets))[string]
                assertNotNull("${tuning.name} string ${6 - string} not heard", reading)
                assertEquals(
                    "${tuning.name} string ${6 - string}",
                    17.0, centsOff(reading!!, tuning.strings[string]).toDouble(), 2.0,
                )
            }
        }
    }

    @Test
    fun separatesTheBStringFromTheLowEsOvertone() {
        // B3 sits almost exactly on the third overtone of E2.
        val offsets = listOf(0.0, 0.0, 0.0, 0.0, -20.0, 0.0)
        for (seed in 1..5) {
            val readings = detector(Tuning.STANDARD).detect(strum(Tuning.STANDARD, offsets, seed))
            assertReads(Tuning.STANDARD, offsets, readings, tolerance = 4.0)
        }
    }

    @Test
    fun readsStringsThatAreWellOutOfTune() {
        val offsets = listOf(-70.0, 60.0, -55.0, 80.0, -65.0, 75.0)
        val readings = detector(Tuning.STANDARD).detect(strum(Tuning.STANDARD, offsets))
        assertReads(Tuning.STANDARD, offsets, readings, tolerance = 5.0)
    }

    @Test
    fun leavesUnplayedStringsEmpty() {
        // Only strings with no overtone of a played string on their own pitch can be told apart
        // from silence: an unplayed high E would hear the A string's third overtone.
        val offsets = listOf(null, null, 5.0, -10.0, null, 15.0)
        val readings = detector(Tuning.STANDARD).detect(strum(Tuning.STANDARD, offsets))
        assertReads(Tuning.STANDARD, offsets, readings, tolerance = 3.0)
    }

    @Test
    fun readsASingleString() {
        val offsets = listOf(null, null, null, 10.0, null, null)
        val readings = detector(Tuning.STANDARD).detect(strum(Tuning.STANDARD, offsets))
        assertReads(Tuning.STANDARD, offsets, readings, tolerance = 3.0)
    }

    @Test
    fun silenceHasNoStrings() {
        val readings = detector(Tuning.STANDARD).detect(FloatArray(16384))
        assertEquals(List(6) { null }, readings)
    }

    @Test
    fun noiseHasNoStrings() {
        val random = Random(7)
        val noise = FloatArray(16384) { (0.05 * (random.nextDouble() * 2 - 1)).toFloat() }
        assertEquals(List(6) { null }, detector(Tuning.STANDARD).detect(noise))
    }

    @Test
    fun fftMatchesADirectTransform() {
        val size = 64
        val random = Random(3)
        val input = DoubleArray(size) { random.nextDouble() * 2 - 1 }
        val re = FloatArray(size) { input[it].toFloat() }
        val im = FloatArray(size)
        Fft(size).transform(re, im)
        for (k in 0 until size) {
            var expectedRe = 0.0
            var expectedIm = 0.0
            for (n in 0 until size) {
                expectedRe += input[n] * cos(2 * PI * k * n / size)
                expectedIm -= input[n] * sin(2 * PI * k * n / size)
            }
            assertEquals(expectedRe, re[k].toDouble(), 1e-3)
            assertEquals(expectedIm, im[k].toDouble(), 1e-3)
        }
    }
}
