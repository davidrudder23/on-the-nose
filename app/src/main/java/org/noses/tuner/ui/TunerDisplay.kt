package org.noses.tuner.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import org.noses.tuner.R
import org.noses.tuner.audio.NoteReading
import org.noses.tuner.audio.Tuning
import org.noses.tuner.audio.nearestNote

private const val MAX_CENTS = 50f
private const val IN_TUNE_CENTS = 5f

/** Degrees the needle swings either side of vertical at ±[MAX_CENTS]. */
private const val SWEEP_DEGREES = 60f

private val InTuneGreen = Color(0xFF2EA043)
private val OutOfTuneRed = Color(0xFFE5484D)

/**
 * Shows the note [frequency] should be tuned towards in [tuning], its string number, and a needle
 * gauge of how flat or sharp it is.
 */
@Composable
fun TunerDisplay(frequency: Float?, tuning: Tuning, modifier: Modifier = Modifier) {
    val reading = remember(frequency, tuning) { frequency?.let { nearestNote(it, tuning) } }
    NoteGauge(reading, holdKey = tuning, modifier = modifier)
}

/**
 * Shows the note of [reading] with a needle gauge of how flat or sharp it is. When [reading] is
 * null the last one stays on screen, until [holdKey] changes. Before there is any reading,
 * [idleName] stands in for the note and [idleHint] for the offset.
 */
@Composable
fun NoteGauge(
    reading: NoteReading?,
    holdKey: Any?,
    modifier: Modifier = Modifier,
    idleName: String = stringResource(R.string.no_pitch),
    idleHint: String = stringResource(R.string.play_a_note),
) {
    var held by remember(holdKey) { mutableStateOf<NoteReading?>(null) }
    LaunchedEffect(reading) { if (reading != null) held = reading }

    val shown = reading ?: held
    val inTune = shown != null && abs(shown.cents) <= IN_TUNE_CENTS
    val accent = if (inTune) InTuneGreen else MaterialTheme.colorScheme.onSurface
    // Each label turns red while the needle sits outside the green zone on its side.
    val labelColor = MaterialTheme.colorScheme.onSurface
    val flatColor = if (shown != null && shown.cents < -IN_TUNE_CENTS) OutOfTuneRed else labelColor
    val sharpColor = if (shown != null && shown.cents > IN_TUNE_CENTS) OutOfTuneRed else labelColor

    Column(
        modifier = modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row {
            Text(
                text = shown?.name ?: idleName,
                style = MaterialTheme.typography.displayLarge,
                color = accent,
                modifier = Modifier.alignByBaseline(),
            )
            if (shown != null) {
                Text(
                    text = shown.octave.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = accent,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
        Text(
            text = shown?.string?.let { stringResource(R.string.string_number, it) } ?: "",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // The gauge is the only flexible part, so it shrinks when the screen is short (landscape).
        Gauge(cents = shown?.cents ?: 0f, needleColor = accent, modifier = Modifier.weight(1f, fill = false))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.flat), style = MaterialTheme.typography.labelLarge, color = flatColor)
            Text(
                text = when {
                    shown == null -> idleHint
                    inTune -> stringResource(R.string.in_tune)
                    else -> stringResource(R.string.cents_offset, shown.cents.roundToInt())
                },
                style = MaterialTheme.typography.titleLarge,
                color = accent,
            )
            Text(stringResource(R.string.sharp), style = MaterialTheme.typography.labelLarge, color = sharpColor)
        }

        Text(
            text = shown?.let { stringResource(R.string.frequency_hz, it.frequency) } ?: "",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Gauge(cents: Float, needleColor: Color, modifier: Modifier = Modifier) {
    val needleCents by animateFloatAsState(
        targetValue = cents.coerceIn(-MAX_CENTS, MAX_CENTS),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "needle",
    )
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier.aspectRatio(1.7f)) {
        val trackWidth = 6.dp.toPx()
        val hubRadius = 8.dp.toPx()
        val pivot = Offset(size.width / 2, size.height - hubRadius)
        val radius = minOf(size.width / 2, pivot.y) - trackWidth

        fun pointAt(centsOffset: Float, distance: Float): Offset {
            val angle = Math.toRadians((centsOffset / MAX_CENTS * SWEEP_DEGREES).toDouble())
            return Offset(pivot.x + distance * sin(angle).toFloat(), pivot.y - distance * cos(angle).toFloat())
        }

        // drawArc measures angles clockwise from 3 o'clock, so straight up is 270°.
        fun arc(color: Color, halfSweep: Float) = drawArc(
            color = color,
            startAngle = 270f - halfSweep,
            sweepAngle = 2 * halfSweep,
            useCenter = false,
            topLeft = Offset(pivot.x - radius, pivot.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(trackWidth, cap = StrokeCap.Round),
        )
        arc(trackColor, SWEEP_DEGREES)
        arc(InTuneGreen, IN_TUNE_CENTS / MAX_CENTS * SWEEP_DEGREES)

        val tickStart = radius - trackWidth * 1.5f
        for (tick in -50..50 step 10) {
            val length = if (tick == 0) 18.dp.toPx() else 9.dp.toPx()
            drawLine(
                color = if (tick == 0) InTuneGreen else tickColor,
                start = pointAt(tick.toFloat(), tickStart),
                end = pointAt(tick.toFloat(), tickStart - length),
                strokeWidth = (if (tick == 0) 3.dp else 2.dp).toPx(),
                cap = StrokeCap.Round,
            )
        }

        drawLine(
            color = needleColor,
            start = pivot,
            end = pointAt(needleCents, tickStart),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(needleColor, hubRadius, pivot)
    }
}
