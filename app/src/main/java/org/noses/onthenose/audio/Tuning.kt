package org.noses.onthenose.audio

/**
 * The notes a guitar's open strings are tuned to, as MIDI note numbers from the lowest-pitched
 * string to the highest. [CHROMATIC] has no strings: every note is a valid target.
 */
enum class Tuning(val strings: List<Int>) {
    CHROMATIC(emptyList()),
    STANDARD(listOf(40, 45, 50, 55, 59, 64)),
    DROP_D(listOf(38, 45, 50, 55, 59, 64)),
    HALF_STEP_DOWN(listOf(39, 44, 49, 54, 58, 63)),
    OPEN_G(listOf(38, 43, 50, 55, 59, 62)),
    OPEN_D(listOf(38, 45, 50, 54, 57, 62)),
    OPEN_E_MINOR(listOf(40, 47, 52, 55, 59, 64)),
    OPEN_D_MINOR(listOf(38, 45, 50, 53, 57, 62)),
    DADGAD(listOf(38, 45, 50, 55, 57, 62));

    /** True for every tuning except [CHROMATIC]. */
    val hasStrings: Boolean get() = strings.isNotEmpty()
}
