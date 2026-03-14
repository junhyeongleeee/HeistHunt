package com.heisthunt.app.ui.voicetest

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.heisthunt.app.viewmodel.VoiceTestViewModel
import com.heisthunt.app.voice.VoiceChannelManager

@Composable
actual fun VoiceTestScreenContainer(onBack: () -> Unit) {
    val context = LocalContext.current
    val voiceChannelManager = remember { VoiceChannelManager(context) }
    val viewModel = remember { VoiceTestViewModel(voiceChannelManager) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        println("🎤 [VoiceTest] RECORD_AUDIO permission: ${if (granted) "GRANTED" else "DENIED"}")
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            println("🎤 [VoiceTest] Requesting RECORD_AUDIO permission...")
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            println("🎤 [VoiceTest] RECORD_AUDIO permission already granted")
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.disconnect() }
    }

    VoiceTestScreen(
        viewModel = viewModel,
        onBack = onBack
    )
}
