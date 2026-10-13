package org.noses.onthenose.audio

import kotlin.math.PI
import kotlin.math.sin

private const val AMPLITUDE = 0.6
private const val FADE_MS = 30

/**
 * Generates a sine tone at [frequency] Hz as 16-bit mono PCM, faded in and out so that it starts
 * and stops without a click. Has no Android dependencies so it can be unit tested on the JVM.
 */
fun toneSamples(frequency: Float, sampleRate: Int, durationMs: Int): ShortArray {
    val count = sampleRate * durationMs / 1000
    val fade = minOf(sampleRate * FADE_MS / 1000, count / 2)
    return ShortArray(count) { i ->
        val envelope = when {
            i < fade -> i.toDouble() / fade
            i >= count - fade -> (count - 1 - i).toDouble() / fade
            else -> 1.0
        }
        (AMPLITUDE * envelope * sin(2 * PI * frequency * i / sampleRate) * Short.MAX_VALUE).toInt().toShort()
    }
}
