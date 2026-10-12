package org.noses.tuner.audio

import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

private val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
private const val A4_MIDI = 69

/** The name of a MIDI note without its octave, such as "C#". */
fun noteName(midi: Int): String = NOTE_NAMES[Math.floorMod(midi, 12)]

/** The frequency in Hz of a MIDI note, relative to [a4] Hz. */
fun midiFrequency(midi: Int, a4: Float = 440f): Float = a4 * 2f.pow((midi - A4_MIDI) / 12f)

/** The note a measured frequency is being tuned towards, and how far the measurement is from it. */
data class NoteReading(
    val name: String,
    val octave: Int,
    /** Guitar string number, counting from 1 at the highest-pitched string. Null when chromatic. */
    val string: Int?,
    /** Frequency of the note itself, in Hz. */
    val noteFrequency: Float,
    /** The measured frequency, in Hz. */
    val frequency: Float,
    /**
     * Offset from the note in cents: negative is flat, positive is sharp. Within ±50 when
     * chromatic, but unbounded when the nearest string of a tuning is further away than that,
     * and up to ±600 when measured against one note in its nearest octave.
     */
    val cents: Float,
)

/**
 * Finds the note to tune [frequency] (which must be positive) towards: the nearest open string
 * of [tuning], or the nearest note of any kind when chromatic. Pitches are relative to [a4] Hz.
 */
fun nearestNote(frequency: Float, tuning: Tuning = Tuning.CHROMATIC, a4: Float = 440f): NoteReading {
    require(frequency > 0f)
    val midi = A4_MIDI + 12 * log2(frequency / a4)
    val stringIndex = tuning.strings.indices.minByOrNull { abs(tuning.strings[it] - midi) }
    val target = stringIndex?.let { tuning.strings[it] } ?: midi.roundToInt()
    return NoteReading(
        name = noteName(target),
        octave = Math.floorDiv(target, 12) - 1,
        string = stringIndex?.let { tuning.strings.size - it },
        noteFrequency = midiFrequency(target, a4),
        frequency = frequency,
        cents = (midi - target) * 100,
    )
}

/**
 * Measures [frequency] (which must be positive) against the note with [pitchClass] (0 is C,
 * 11 is B) in whichever octave is nearest, so the same target suits any vocal range.
 */
fun noteInNearestOctave(frequency: Float, pitchClass: Int, a4: Float = 440f): NoteReading {
    require(frequency > 0f)
    require(pitchClass in 0..11)
    val midi = A4_MIDI + 12 * log2(frequency / a4)
    val target = pitchClass + 12 * ((midi - pitchClass) / 12).roundToInt()
    return NoteReading(
        name = noteName(target),
        octave = Math.floorDiv(target, 12) - 1,
        string = null,
        noteFrequency = midiFrequency(target, a4),
        frequency = frequency,
        cents = (midi - target) * 100,
    )
}
