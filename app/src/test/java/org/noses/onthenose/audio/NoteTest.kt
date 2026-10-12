package org.noses.onthenose.audio

import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteTest {
    private fun label(frequency: Double) = nearestNote(frequency.toFloat()).let { "${it.name}${it.octave}" }

    @Test
    fun namesGuitarOpenStrings() {
        val strings = listOf(82.41 to "E2", 110.0 to "A2", 146.83 to "D3", 196.0 to "G3", 246.94 to "B3", 329.63 to "E4")
        for ((frequency, expected) in strings) {
            assertEquals(expected, label(frequency))
            assertEquals(0f, nearestNote(frequency.toFloat()).cents, 0.5f)
        }
    }

    @Test
    fun namesSharpsAndOctaveBoundaries() {
        assertEquals("C4", label(261.63))
        assertEquals("C#4", label(277.18))
        assertEquals("B3", label(246.94))
        assertEquals("A#1", label(58.27))
    }

    @Test
    fun reportsCentsFlatAndSharp() {
        val sharp = nearestNote(440f * 2f.pow(20 / 1200f))
        assertEquals("A", sharp.name)
        assertEquals(20f, sharp.cents, 0.1f)
        assertEquals(440f, sharp.noteFrequency, 0.01f)

        val flat = nearestNote(440f * 2f.pow(-35 / 1200f))
        assertEquals("A", flat.name)
        assertEquals(-35f, flat.cents, 0.1f)
    }

    @Test
    fun switchesToNextNotePastHalfway() {
        val reading = nearestNote(440f * 2f.pow(60 / 1200f))
        assertEquals("A#", reading.name)
        assertEquals(-40f, reading.cents, 0.1f)
    }
}
