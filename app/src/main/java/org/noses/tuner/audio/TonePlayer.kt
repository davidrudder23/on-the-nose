package org.noses.tuner.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.delay

private const val SAMPLE_RATE = 44100

/** How long after the tone ends the microphone can still be hearing it. */
private const val ECHO_MS = 250L

/**
 * Plays a tone at [frequency] Hz through the speaker, returning once it has finished and died
 * away. Cancelling the caller stops the tone.
 */
suspend fun playTone(frequency: Float, durationMs: Int = 1500) {
    val samples = toneSamples(frequency, SAMPLE_RATE, durationMs)
    val track = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
        )
        .setTransferMode(AudioTrack.MODE_STATIC)
        .setBufferSizeInBytes(samples.size * Short.SIZE_BYTES)
        .build()
    try {
        track.write(samples, 0, samples.size)
        track.play()
        delay(durationMs + ECHO_MS)
    } finally {
        track.release()
    }
}
