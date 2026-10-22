package org.noses.onthenose.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Measures several strings ringing at once, such as a strum across all six. Each string is
 * looked for only near its own [targets] frequency, which is what makes this tractable: the
 * detector never has to work out which notes are playing, only how far each expected one is off.
 *
 * Each candidate pitch is scored by adding up the spectrum at all of its overtones. A string's own
 * overtones all line up at its true pitch, so that pitch outscores a stray overtone of another
 * string that happens to land nearby, and overtones that land on another string's are left out
 * (see [ownHarmonics]).
 *
 * Limits: a string an octave above another (the Ds of Open D) shares every overtone with it, so
 * in a strum their readings pull toward each other unless both are close to in tune. And a string
 * that isn't ringing reads as present when another string's overtone sits on its pitch: an
 * unplayed high E hears the A string's third overtone.
 *
 * Has no Android dependencies so it can be unit tested on the JVM.
 */
class StringsDetector(
    private val sampleRate: Int,
    private val targets: List<Float>,
    /** About 0.37 s at 44.1 kHz, long enough to tell low strings a few cents apart. */
    val windowSize: Int = 16384,
) {
    // Zero padding to twice the window makes the spectrum smooth enough to read between bins.
    private val fftSize = windowSize * 2
    private val fft = Fft(fftSize)
    private val re = FloatArray(fftSize)
    private val im = FloatArray(fftSize)
    private val magnitude = FloatArray(fftSize / 2)
    private val hann = FloatArray(windowSize) { i ->
        (0.5 - 0.5 * cos(2 * PI * i / (windowSize - 1))).toFloat()
    }
    private var noiseFloor = 1f
    private val harmonics = targets.indices.map { ownHarmonics(it) }
    private val scores = FloatArray(2 * SEARCH_CENTS + 1)
    private val floorBins = (binOf(FLOOR_MIN_HZ).toInt()..binOf(FLOOR_MAX_HZ).toInt()).let { range ->
        FloatArray(range.last - range.first + 1) to range.first
    }

    init {
        require(Integer.bitCount(windowSize) == 1) { "windowSize must be a power of two" }
    }

    /**
     * Returns the measured frequency in Hz of each of [targets], in the same order, or null for a
     * string that isn't ringing loudly enough to measure (or is more than a semitone off).
     */
    fun detect(samples: FloatArray): List<Float?> {
        require(samples.size >= windowSize)

        var energy = 0f
        for (i in 0 until windowSize) energy += samples[i] * samples[i]
        if (sqrt(energy / windowSize) < SILENCE_RMS) return targets.map { null }

        for (i in 0 until windowSize) re[i] = samples[i] * hann[i]
        re.fill(0f, windowSize, fftSize)
        im.fill(0f)
        fft.transform(re, im)
        for (k in magnitude.indices) magnitude[k] = sqrt(re[k] * re[k] + im[k] * im[k])

        noiseFloor = noiseFloor()
        return targets.indices.map { measure(it) }
    }

    /**
     * The overtones (1 is the fundamental) that measure string [index]: all up to [HARMONICS]
     * except those that land on another string's overtone and aren't clearly the louder of the
     * two. Overtones fade as they go up, so this string's overtone h wins only over the other's
     * 2h or higher: B3's fundamental keeps the spot over E2's third overtone, but A2's fourth and
     * D3's third are too alike and neither string uses them.
     */
    private fun ownHarmonics(index: Int): IntArray = (1..HARMONICS).filter { h ->
        targets.indices.none { other ->
            other != index && (1 until 2 * h).any { k ->
                abs(1200 * log2(targets[index] * h / (targets[other] * k))) < COLLISION_CENTS
            }
        }
    }.toIntArray()

    private fun measure(index: Int): Float? {
        val target = targets[index]
        val harmonics = harmonics[index]
        var best = 0
        for (step in scores.indices) {
            scores[step] = score(target * centsRatio(step - SEARCH_CENTS), harmonics)
            if (scores[step] > scores[best]) best = step
        }
        // A best score at the edge of the search is still rising beyond it, toward another note.
        if (best == 0 || best == scores.lastIndex) return null

        val a = scores[best - 1]
        val b = scores[best]
        val c = scores[best + 1]
        val denominator = a - 2 * b + c
        val offset = if (denominator == 0f) 0f else (a - c) / (2 * denominator)
        val frequency = target * centsRatio(best - SEARCH_CENTS + offset)

        // The string must stand clear of the background in its fundamental or first overtones.
        var loudest = 0f
        for (h in harmonics) if (h <= PRESENCE_HARMONICS) loudest = maxOf(loudest, magnitudeAt(frequency * h))
        return if (loudest >= noiseFloor * PRESENCE_RATIO) frequency else null
    }

    private fun score(frequency: Float, harmonics: IntArray): Float {
        var sum = 0f
        for (h in harmonics) {
            val overtone = frequency * h
            if (overtone > MAX_OVERTONE_HZ) break
            sum += ln(1 + magnitudeAt(overtone) / noiseFloor)
        }
        return sum
    }

    /**
     * The spectrum at [frequency], read between bins along a Catmull-Rom curve. A straight line
     * between bins would put every peak exactly on a bin, which is up to 14 cents out for low E.
     */
    private fun magnitudeAt(frequency: Float): Float {
        val bin = binOf(frequency)
        val k = bin.toInt()
        if (k < 1 || k + 2 >= magnitude.size) return 0f
        val t = bin - k
        val p0 = magnitude[k - 1]
        val p1 = magnitude[k]
        val p2 = magnitude[k + 1]
        val p3 = magnitude[k + 2]
        return p1 + 0.5f * t * (p2 - p0 + t * (2 * p0 - 5 * p1 + 4 * p2 - p3 + t * (3 * (p1 - p2) + p3 - p0)))
    }

    /** The median magnitude across the guitar's range: a typical level for bins with no string. */
    private fun noiseFloor(): Float {
        val (buffer, start) = floorBins
        System.arraycopy(magnitude, start, buffer, 0, buffer.size)
        buffer.sort()
        return buffer[buffer.size / 2]
    }

    private fun binOf(frequency: Float) = frequency * fftSize / sampleRate

    private fun centsRatio(cents: Float) = 2f.pow(cents / 1200f)
    private fun centsRatio(cents: Int) = centsRatio(cents.toFloat())

    private companion object {
        /** How far either side of each target to look, in cents. Neighbouring strings are at least 200 apart. */
        const val SEARCH_CENTS = 100
        const val HARMONICS = 6
        const val MAX_OVERTONE_HZ = 5000f
        const val PRESENCE_HARMONICS = 2
        const val COLLISION_CENTS = 100
        const val PRESENCE_RATIO = 30f
        const val SILENCE_RMS = 0.005f
        const val FLOOR_MIN_HZ = 60f
        const val FLOOR_MAX_HZ = 2000f
    }
}

/** An in-place radix-2 fast Fourier transform of a fixed power-of-two [size]. */
internal class Fft(private val size: Int) {
    private val cosTable = FloatArray(size / 2) { cos(2 * PI * it / size).toFloat() }
    private val sinTable = FloatArray(size / 2) { -sin(2 * PI * it / size).toFloat() }
    private val reversed = IntArray(size).also { table ->
        val bits = Integer.numberOfTrailingZeros(size)
        for (i in 0 until size) table[i] = Integer.reverse(i) ushr (32 - bits)
    }

    init {
        require(size >= 2 && Integer.bitCount(size) == 1) { "size must be a power of two" }
    }

    fun transform(re: FloatArray, im: FloatArray) {
        for (i in 0 until size) {
            val j = reversed[i]
            if (j > i) {
                var t = re[i]; re[i] = re[j]; re[j] = t
                t = im[i]; im[i] = im[j]; im[j] = t
            }
        }
        var length = 2
        while (length <= size) {
            val half = length / 2
            val stride = size / length
            for (start in 0 until size step length) {
                for (k in 0 until half) {
                    val wr = cosTable[k * stride]
                    val wi = sinTable[k * stride]
                    val a = start + k
                    val b = a + half
                    val tr = re[b] * wr - im[b] * wi
                    val ti = re[b] * wi + im[b] * wr
                    re[b] = re[a] - tr
                    im[b] = im[a] - ti
                    re[a] += tr
                    im[a] += ti
                }
            }
            length *= 2
        }
    }
}
