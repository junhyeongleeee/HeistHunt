package com.heisthunt.app.ui.voicetest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.heisthunt.app.viewmodel.VoiceTestViewModel
import com.heisthunt.app.voice.VoiceChannelManager

@Composable
actual fun VoiceTestScreenContainer(onBack: () -> Unit) {
    val voiceChannelManager = remember { VoiceChannelManager() }
    val viewModel = remember { VoiceTestViewModel(voiceChannelManager) }

    VoiceTestScreen(
        viewModel = viewModel,
        onBack = onBack
    )
}
