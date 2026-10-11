package org.noses.tuner.audio

import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TuningTest {
    private fun cents(frequency: Double, offset: Int) = (frequency * 2.0.pow(offset / 1200.0)).toFloat()

    @Test
    fun standardNumbersStringsFromTheHighest() {
        val strings = listOf(82.41 to 6, 110.0 to 5, 146.83 to 4, 196.0 to 3, 246.94 to 2, 329.63 to 1)
        val names = listOf("E", "A", "D", "G", "B", "E")
        strings.forEachIndexed { i, (frequency, string) ->
            val reading = nearestNote(frequency.toFloat(), Tuning.STANDARD)
            assertEquals(names[i], reading.name)
            assertEquals(string, reading.string)
            assertEquals(0f, reading.cents, 0.5f)
        }
    }

    @Test
    fun tunesTowardsNearestStringRatherThanNearestNote() {
        // A smidge above C4 is nearer to the B string than to the high E string.
        val reading = nearestNote(cents(261.63, 5), Tuning.STANDARD)
        assertEquals("B", reading.name)
        assertEquals(3, reading.octave)
        assertEquals(2, reading.string)
        assertEquals(105f, reading.cents, 0.5f)
    }

    @Test
    fun pitchesBeyondTheOuterStringsUseThoseStrings() {
        val low = nearestNote(60f, Tuning.STANDARD)
        assertEquals(6, low.string)
        assertEquals(true, low.cents < -500f)

        val high = nearestNote(700f, Tuning.STANDARD)
        assertEquals(1, high.string)
        assertEquals(true, high.cents > 1200f)
    }

    @Test
    fun dropDLowersOnlyTheSixthString() {
        val sixth = nearestNote(73.42f, Tuning.DROP_D)
        assertEquals("D", sixth.name)
        assertEquals(2, sixth.octave)
        assertEquals(6, sixth.string)
        assertEquals(5, nearestNote(110f, Tuning.DROP_D).string)
    }

    @Test
    fun openMinorTuningsSpellMinorChords() {
        fun notes(tuning: Tuning) = tuning.strings.map(::noteName)
        assertEquals(listOf("E", "B", "E", "G", "B", "E"), notes(Tuning.OPEN_E_MINOR))
        assertEquals(listOf("D", "A", "D", "F", "A", "D"), notes(Tuning.OPEN_D_MINOR))
    }

    @Test
    fun chromaticHasNoString() {
        val reading = nearestNote(cents(261.63, 5), Tuning.CHROMATIC)
        assertEquals("C", reading.name)
        assertNull(reading.string)
    }

    @Test
    fun everyTuningHasSixAscendingStrings() {
        for (tuning in Tuning.entries.filter { it != Tuning.CHROMATIC }) {
            assertEquals(tuning.name, 6, tuning.strings.size)
            assertEquals(tuning.name, tuning.strings.sorted(), tuning.strings)
        }
    }
}
