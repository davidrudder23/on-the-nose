package org.noses.onthenose.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive

private const val SAMPLE_RATE = 44100
private const val PITCH_HOP_SIZE = 1024
private const val STRINGS_HOP_SIZE = 2048

/**
 * Records from the microphone while collected and emits the detected pitch in Hz
 * (null when nothing pitched is heard) about 43 times a second.
 * The caller must hold RECORD_AUDIO.
 */
fun pitchFlow(): Flow<Float?> = flow {
    val detector = PitchDetector(SAMPLE_RATE)
    emitAll(micWindows(detector.windowSize, PITCH_HOP_SIZE).map(detector::detect))
}.flowOn(Dispatchers.Default).conflate()

/**
 * Records from the microphone while collected and emits the measured frequency in Hz of each
 * string of [tuning], lowest first (null for a string not heard), about 21 times a second.
 * The caller must hold RECORD_AUDIO.
 */
fun stringsFlow(tuning: Tuning): Flow<List<Float?>> = flow {
    val detector = StringsDetector(SAMPLE_RATE, tuning.strings.map { midiFrequency(it) })
    emitAll(micWindows(detector.windowSize, STRINGS_HOP_SIZE).map(detector::detect))
}.flowOn(Dispatchers.Default).conflate()

/**
 * Emits the latest [windowSize] samples from the microphone every [hopSize] samples. Each window
 * is a fresh array. Windows the collector is too slow for are dropped, so recording never stalls.
 */
@SuppressLint("MissingPermission")
private fun micWindows(windowSize: Int, hopSize: Int): Flow<FloatArray> = flow {
    val minBufferBytes = AudioRecord.getMinBufferSize(
        SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT,
    )
    val record = AudioRecord(
        // VOICE_RECOGNITION skips the gain control and noise suppression applied to MIC.
        MediaRecorder.AudioSource.VOICE_RECOGNITION,
        SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_FLOAT,
        maxOf(minBufferBytes, windowSize * Float.SIZE_BYTES),
    )
    try {
        if (record.state != AudioRecord.STATE_INITIALIZED) return@flow
        record.startRecording()

        val window = FloatArray(windowSize)
        val hop = FloatArray(hopSize)
        while (currentCoroutineContext().isActive) {
            var filled = 0
            while (filled < hopSize) {
                val read = record.read(hop, filled, hopSize - filled, AudioRecord.READ_BLOCKING)
                if (read <= 0) return@flow
                filled += read
            }
            System.arraycopy(window, hopSize, window, 0, window.size - hopSize)
            System.arraycopy(hop, 0, window, window.size - hopSize, hopSize)
            emit(window.copyOf())
        }
    } finally {
        record.release()
    }
}.flowOn(Dispatchers.IO).conflate()
