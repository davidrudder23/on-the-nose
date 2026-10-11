package org.noses.tuner.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

private const val SAMPLE_RATE = 44100
private const val HOP_SIZE = 1024

/**
 * Records from the microphone while collected and emits the detected pitch in Hz
 * (null when nothing pitched is heard) roughly every [HOP_SIZE] samples.
 * The caller must hold RECORD_AUDIO.
 */
@SuppressLint("MissingPermission")
fun pitchFlow(): Flow<Float?> = flow {
    val detector = PitchDetector(SAMPLE_RATE)
    val minBufferBytes = AudioRecord.getMinBufferSize(
        SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT,
    )
    val record = AudioRecord(
        // VOICE_RECOGNITION skips the gain control and noise suppression applied to MIC.
        MediaRecorder.AudioSource.VOICE_RECOGNITION,
        SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_FLOAT,
        maxOf(minBufferBytes, detector.windowSize * Float.SIZE_BYTES),
    )
    try {
        if (record.state != AudioRecord.STATE_INITIALIZED) return@flow
        record.startRecording()

        val window = FloatArray(detector.windowSize)
        val hop = FloatArray(HOP_SIZE)
        while (currentCoroutineContext().isActive) {
            var filled = 0
            while (filled < HOP_SIZE) {
                val read = record.read(hop, filled, HOP_SIZE - filled, AudioRecord.READ_BLOCKING)
                if (read <= 0) return@flow
                filled += read
            }
            System.arraycopy(window, HOP_SIZE, window, 0, window.size - HOP_SIZE)
            System.arraycopy(hop, 0, window, window.size - HOP_SIZE, HOP_SIZE)
            emit(detector.detect(window))
        }
    } finally {
        record.release()
    }
}.flowOn(Dispatchers.Default).conflate()
