package org.noses.tuner.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.noses.tuner.R
import org.noses.tuner.audio.Tuning
import org.noses.tuner.audio.noteName

/** A button showing the [selected] tuning that opens a menu of all tunings. */
@Composable
fun TuningPicker(selected: Tuning, onSelect: (Tuning) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        TextButton(onClick = { expanded = true }) {
            Text(
                text = stringResource(R.string.tuning_button, tuningLabel(selected)),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for (tuning in Tuning.entries) {
                DropdownMenuItem(
                    text = { Text(tuningLabel(tuning)) },
                    onClick = {
                        expanded = false
                        onSelect(tuning)
                    },
                )
            }
        }
    }
}

/** The tuning's name followed by its string notes from lowest to highest, e.g. "Standard · E A D G B E". */
@Composable
private fun tuningLabel(tuning: Tuning): String {
    val name = stringResource(
        when (tuning) {
            Tuning.CHROMATIC -> R.string.tuning_chromatic
            Tuning.STANDARD -> R.string.tuning_standard
            Tuning.DROP_D -> R.string.tuning_drop_d
            Tuning.HALF_STEP_DOWN -> R.string.tuning_half_step_down
            Tuning.OPEN_G -> R.string.tuning_open_g
            Tuning.OPEN_D -> R.string.tuning_open_d
            Tuning.OPEN_E_MINOR -> R.string.tuning_open_e_minor
            Tuning.OPEN_D_MINOR -> R.string.tuning_open_d_minor
            Tuning.DADGAD -> R.string.tuning_dadgad
        }
    )
    if (tuning.strings.isEmpty()) return name
    return stringResource(R.string.tuning_with_notes, name, tuning.strings.joinToString(" ", transform = ::noteName))
}
