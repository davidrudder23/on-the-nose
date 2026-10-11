package org.noses.tuner.audio

import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

private val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
private const val A4_MIDI = 69

/** The equal-tempered note nearest to a measured frequency, and how far the measurement is from it. */
data class NoteReading(
    val name: String,
    val octave: Int,
    /** Frequency of the note itself, in Hz. */
    val noteFrequency: Float,
    /** The measured frequency, in Hz. */
    val frequency: Float,
    /** Offset from the note in cents: negative is flat, positive is sharp. Always within ±50. */
    val cents: Float,
)

/** Finds the note nearest to [frequency] (which must be positive), tuned relative to [a4] Hz. */
fun nearestNote(frequency: Float, a4: Float = 440f): NoteReading {
    require(frequency > 0f)
    val midi = A4_MIDI + 12 * log2(frequency / a4)
    val nearest = midi.roundToInt()
    return NoteReading(
        name = NOTE_NAMES[Math.floorMod(nearest, 12)],
        octave = Math.floorDiv(nearest, 12) - 1,
        noteFrequency = a4 * 2f.pow((nearest - A4_MIDI) / 12f),
        frequency = frequency,
        cents = (midi - nearest) * 100,
    )
}
