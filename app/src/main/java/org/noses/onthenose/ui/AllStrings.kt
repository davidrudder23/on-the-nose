package org.noses.onthenose.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import org.noses.onthenose.R
import org.noses.onthenose.audio.Tuning
import org.noses.onthenose.audio.midiFrequency
import org.noses.onthenose.audio.noteName

/** How long a string's last reading stays up after it stops being heard, as it rings out. */
private const val HOLD_MILLIS = 3000L

/** Cents off at which each light beyond the green middle one takes over, nearest first. */
private val LIGHT_STEPS = floatArrayOf(IN_TUNE_CENTS, 15f, 30f)
private const val LIGHTS = 7
private const val MIDDLE = LIGHTS / 2

private val NearAmber = Color(0xFFE5A50A)
private val FarOrange = Color(0xFFF07F2D)

private val RowHeight = 56.dp
private val RowGap = 12.dp

/**
 * One row per string of [tuning], lowest string first, each lighting one of seven lights for how
 * far the matching entry of [frequencies] (null when that string isn't heard) is from it.
 */
@Composable
fun AllStrings(frequencies: List<Float?>, tuning: Tuning, modifier: Modifier = Modifier) {
    val strings = tuning.strings
    BoxWithConstraints(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Short screens (landscape) split the strings into two columns of three.
        val columns = if (maxHeight < (RowHeight + RowGap) * strings.size) 2 else 1
        val perColumn = (strings.size + columns - 1) / columns
        Row(
            modifier = Modifier.widthIn(max = 560.dp * columns).fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            for (column in strings.indices.chunked(perColumn)) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(RowGap, Alignment.CenterVertically),
                ) {
                    for (index in column) {
                        StringRow(
                            midi = strings[index],
                            string = strings.size - index,
                            frequency = frequencies.getOrNull(index),
                            holdKey = tuning,
                            modifier = Modifier.height(RowHeight).fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * A row for one string: its number and note, seven lights running from very flat through in tune
 * (the middle one) to very sharp, and the offset in cents. The row turns green when the string is in tune.
 */
@Composable
private fun StringRow(midi: Int, string: Int, frequency: Float?, holdKey: Any?, modifier: Modifier = Modifier) {
    val cents = frequency?.let { 1200 * log2(it / midiFrequency(midi)) }

    // A string rings out between strums, so keep its last reading up for a while, not a flicker.
    var held by remember(holdKey) { mutableStateOf<Float?>(null) }
    LaunchedEffect(cents) {
        if (cents != null) {
            held = cents
        } else {
            delay(HOLD_MILLIS)
            held = null
        }
    }

    val shown = cents ?: held
    val lit = shown?.let { lightFor(it) }
    val inTune = lit == MIDDLE
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val container by animateColorAsState(
        if (inTune) InTuneGreen.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainer,
        label = "container",
    )
    val name = noteName(midi)
    val octave = Math.floorDiv(midi, 12) - 1
    val offset = when {
        shown == null -> stringResource(R.string.no_pitch)
        inTune -> stringResource(R.string.in_tune)
        else -> stringResource(R.string.cents_offset, shown.roundToInt())
    }
    val description = stringResource(R.string.string_number, string) + ", $name$octave, " + when {
        shown == null -> stringResource(R.string.no_pitch)
        inTune -> stringResource(R.string.in_tune)
        shown < 0 -> stringResource(R.string.cents_flat, abs(shown).roundToInt())
        else -> stringResource(R.string.cents_sharp, shown.roundToInt())
    }

    Surface(
        color = container,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StringBadge(string)
            Row(Modifier.width(48.dp)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (inTune) InTuneGreen else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    text = octave.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    color = muted,
                    modifier = Modifier.alignByBaseline(),
                )
            }
            Row(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (light in 0 until LIGHTS) {
                    Light(
                        color = lightColor(light),
                        on = light == lit,
                        modifier = Modifier.weight(1f).height(22.dp),
                    )
                }
            }
            Text(
                text = offset,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (inTune) InTuneGreen else muted,
                textAlign = TextAlign.End,
                modifier = Modifier.width(64.dp),
            )
        }
    }
}

/**
 * Which light [cents] turns on: [MIDDLE] within [IN_TUNE_CENTS], one step out per threshold in
 * [LIGHT_STEPS] passed, so 0 is very flat and the last light very sharp.
 */
private fun lightFor(cents: Float): Int {
    val steps = LIGHT_STEPS.count { abs(cents) > it }
    return if (cents < 0) MIDDLE - steps else MIDDLE + steps
}

private fun lightColor(light: Int): Color = when (abs(light - MIDDLE)) {
    0 -> InTuneGreen
    1 -> NearAmber
    2 -> FarOrange
    else -> OutOfTuneRed
}

/** One LED: a faint outline of its colour when off, solid when on. */
@Composable
private fun Light(color: Color, on: Boolean, modifier: Modifier = Modifier) {
    val fill by animateColorAsState(if (on) color else color.copy(alpha = 0.10f), label = "light")
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier
            .background(fill, shape)
            .border(1.5.dp, color.copy(alpha = if (on) 1f else 0.35f), shape),
    )
}

private val BadgeSize = 28.dp

@Composable
private fun StringBadge(string: Int) {
    Box(
        modifier = Modifier
            .size(BadgeSize)
            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = string.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
