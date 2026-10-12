package org.noses.tuner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.random.Random
import kotlinx.coroutines.launch
import org.noses.tuner.R
import org.noses.tuner.audio.midiFrequency
import org.noses.tuner.audio.noteInNearestOctave
import org.noses.tuner.audio.noteName
import org.noses.tuner.audio.playTone

/** MIDI number of C4. The target note is played in the octave above it, which phone speakers carry well. */
private const val PLAYBACK_OCTAVE = 60

/**
 * Picks a random note for the user to sing and shows how close [frequency] is to it, in whichever
 * octave they sing it. Buttons play the note aloud and pick a new one.
 */
@Composable
fun VoiceTraining(frequency: Float?, modifier: Modifier = Modifier) {
    // The target is a pitch class: 0 is C, 11 is B.
    var target by rememberSaveable { mutableIntStateOf(Random.nextInt(12)) }
    var playing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // The microphone hears the phone's own speaker, so nothing is measured while the note plays.
    val reading = remember(frequency, target, playing) {
        if (playing) null else frequency?.let { noteInNearestOctave(it, target) }
    }

    Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.sing_this_note),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            NoteGauge(
                reading = reading,
                holdKey = target,
                idleName = noteName(target),
                idleHint = stringResource(R.string.sing_the_note),
            )
        }
        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Button(
                enabled = !playing,
                onClick = {
                    scope.launch {
                        playing = true
                        try {
                            playTone(midiFrequency(PLAYBACK_OCTAVE + target))
                        } finally {
                            playing = false
                        }
                    }
                },
            ) {
                Text(stringResource(R.string.play_note))
            }
            // Offsetting by 1..11 always lands on a different note.
            OutlinedButton(onClick = { target = (target + 1 + Random.nextInt(11)) % 12 }) {
                Text(stringResource(R.string.new_note))
            }
        }
    }
}
