package org.noses.tuner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.noses.tuner.ads.BannerAd
import org.noses.tuner.audio.Tuning
import org.noses.tuner.audio.pitchFlow
import org.noses.tuner.ui.TunerDisplay
import org.noses.tuner.ui.TuningPicker

private const val TUNING_KEY = "tuning"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch(Dispatchers.IO) { MobileAds.initialize(this@MainActivity) }
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
        Scaffold(
            bottomBar = { BannerAd(Modifier.fillMaxWidth().navigationBarsPadding()) },
        ) { innerPadding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                if (hasMicPermission) {
                    LiveTuner()
                } else {
                    MicPermissionPrompt(
                        onRequest = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveTuner() {
    // Collecting with the lifecycle releases the microphone whenever the app is backgrounded.
    val frequency by remember { pitchFlow() }.collectAsStateWithLifecycle(initialValue = null)

    val context = LocalContext.current
    val settings = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var tuning by remember {
        val saved = settings.getString(TUNING_KEY, null)
        mutableStateOf(Tuning.entries.find { it.name == saved } ?: Tuning.STANDARD)
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        TuningPicker(
            selected = tuning,
            onSelect = {
                tuning = it
                settings.edit().putString(TUNING_KEY, it.name).apply()
            },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
        )
        TunerDisplay(frequency, tuning)
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
