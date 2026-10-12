package org.noses.tuner

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.noses.tuner.audio.Tuning
import org.noses.tuner.audio.pitchFlow
import org.noses.tuner.ui.TunerDisplay
import org.noses.tuner.ui.TuningPicker
import org.noses.tuner.ui.VoiceTraining

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
    VOICE_TRAINING(R.string.view_voice_training),
}

@Composable
private fun AppViews() {
    // Collecting with the lifecycle releases the microphone whenever the app is backgrounded.
    val frequency by remember { pitchFlow() }.collectAsStateWithLifecycle(initialValue = null)
    // Deliberately not saved anywhere, not even in saved instance state: every time the app is
    // opened it starts on the tuner with the chromatic scale. Rotation keeps these because the
    // manifest stops the activity being recreated for it.
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
            when (view) {
                AppView.TUNER -> GuitarTuner(frequency, tuning, onTuningChange = { tuning = it })
                AppView.VOICE_TRAINING -> VoiceTraining(frequency)
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
