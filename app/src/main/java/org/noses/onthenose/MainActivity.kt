package org.noses.onthenose

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.noses.onthenose.audio.Tuning
import org.noses.onthenose.audio.pitchFlow
import org.noses.onthenose.audio.stringsFlow
import org.noses.onthenose.ui.AllStrings
import org.noses.onthenose.ui.TunerDisplay
import org.noses.onthenose.ui.TuningPicker
import org.noses.onthenose.ui.VoiceTraining

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TunerApp() }
    }
}

@Composable
fun TunerApp() {
    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasMicPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasMicPermission) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        Scaffold { innerPadding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                if (hasMicPermission) {
                    AppViews()
                } else {
                    MicPermissionPrompt(
                        onRequest = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    )
                }
            }
        }
    }
}

/** The screens the app switches between, in tab order. */
private enum class AppView(@StringRes val label: Int) {
    TUNER(R.string.view_tuner),
    ALL_STRINGS(R.string.view_all_strings),
    VOICE_TRAINING(R.string.view_voice_training),
}

@Composable
private fun AppViews() {
    // Deliberately not saved anywhere, not even in saved instance state: every time the app is
    // opened it starts on the tuner with the chromatic scale. Rotation keeps these because the
    // manifest stops the activity being recreated for it. The tuner and the all-strings view
    // share the tuning, so picking one in either carries over to the other.
    var view by remember { mutableStateOf(AppView.TUNER) }
    var tuning by remember { mutableStateOf(Tuning.CHROMATIC) }

    Column(Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = view.ordinal) {
            for (entry in AppView.entries) {
                Tab(
                    selected = view == entry,
                    onClick = { view = entry },
                    text = { Text(stringResource(entry.label)) },
                )
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (view == AppView.ALL_STRINGS) {
                // Chromatic has no strings to show, so this view falls back to standard tuning.
                AllStringsTuner(
                    tuning = if (tuning.hasStrings) tuning else Tuning.STANDARD,
                    onTuningChange = { tuning = it },
                )
            } else {
                // Collecting with the lifecycle releases the microphone whenever the app is
                // backgrounded. The tuner and voice training share this, so switching between
                // them keeps the microphone open.
                val frequency by remember { pitchFlow() }.collectAsStateWithLifecycle(initialValue = null)
                when (view) {
                    AppView.TUNER -> GuitarTuner(frequency, tuning, onTuningChange = { tuning = it })
                    AppView.VOICE_TRAINING -> VoiceTraining(frequency)
                    AppView.ALL_STRINGS -> Unit
                }
            }
        }
    }
}

@Composable
private fun GuitarTuner(frequency: Float?, tuning: Tuning, onTuningChange: (Tuning) -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        TuningPicker(
            selected = tuning,
            onSelect = onTuningChange,
            modifier = Modifier.padding(top = 8.dp),
        )
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            TunerDisplay(frequency, tuning)
        }
    }
}

@Composable
private fun AllStringsTuner(tuning: Tuning, onTuningChange: (Tuning) -> Unit) {
    val frequencies by remember(tuning) { stringsFlow(tuning) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        TuningPicker(
            selected = tuning,
            onSelect = onTuningChange,
            modifier = Modifier.padding(top = 8.dp),
            options = Tuning.entries.filter { it.hasStrings },
        )
        // Landscape needs every bit of height for the gauges.
        if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) {
            Text(
                text = stringResource(R.string.strum_all_strings),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AllStrings(frequencies, tuning, Modifier.weight(1f))
    }
}

@Composable
private fun MicPermissionPrompt(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.mic_rationale),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRequest) {
            Text(stringResource(R.string.mic_grant))
        }
    }
}
