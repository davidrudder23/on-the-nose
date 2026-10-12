package org.noses.tuner.audio

import kotlin.math.abs
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceTrainingTest {
    private val a = 9
    private val c = 0

    @Test
    fun matchesTheTargetNoteInAnyOctave() {
        for ((frequency, octave) in listOf(110f to 2, 220f to 3, 440f to 4, 880f to 5)) {
            val reading = noteInNearestOctave(frequency, a)
            assertEquals("A", reading.name)
            assertEquals(octave, reading.octave)
            assertEquals(0f, reading.cents, 0.1f)
            assertNull(reading.string)
        }
    }

    @Test
    fun measuresAgainstTheTargetRatherThanTheNearestNote() {
        // A4 is three semitones below the nearest C, which is C5.
        val reading = noteInNearestOctave(440f, c)
        assertEquals("C", reading.name)
        assertEquals(5, reading.octave)
        assertEquals(-300f, reading.cents, 0.1f)
        assertEquals(523.25f, reading.noteFrequency, 0.01f)
    }

    @Test
    fun reportsSmallOffsetsFlatAndSharp() {
        assertEquals(20f, noteInNearestOctave(220f * 2f.pow(20 / 1200f), a).cents, 0.1f)
        assertEquals(-35f, noteInNearestOctave(220f * 2f.pow(-35 / 1200f), a).cents, 0.1f)
    }

    @Test
    fun neverMoreThanHalfAnOctaveAway() {
        var frequency = 70f
        while (frequency < 1200f) {
            for (pitchClass in 0..11) {
                assertTrue(abs(noteInNearestOctave(frequency, pitchClass).cents) <= 600.01f)
            }
            frequency *= 1.03f
        }
    }

    @Test
    fun midiFrequencyMatchesConcertPitch() {
        assertEquals(440f, midiFrequency(69), 0.001f)
        assertEquals(261.63f, midiFrequency(60), 0.01f)
    }

    @Test
    fun toneIsDetectedAtItsFrequency() {
        val sampleRate = 44100
        val detector = PitchDetector(sampleRate)
        for (midi in 60..71) {
            val expected = midiFrequency(midi)
            val tone = toneSamples(expected, sampleRate, durationMs = 500)
            // Skip the fade-in so the window is at full volume.
            val window = FloatArray(detector.windowSize) { tone[4000 + it] / Short.MAX_VALUE.toFloat() }
            assertEquals(expected, detector.detect(window)!!, 1f)
        }
    }

    @Test
    fun toneFadesInAndOut() {
        val tone = toneSamples(440f, sampleRate = 44100, durationMs = 500)
        assertEquals(22050, tone.size)
        assertEquals(0, tone.first().toInt())
        assertTrue(abs(tone.last().toInt()) < 50)
        assertTrue(tone.maxOf { abs(it.toInt()) } > 0.55 * Short.MAX_VALUE)
    }
}
