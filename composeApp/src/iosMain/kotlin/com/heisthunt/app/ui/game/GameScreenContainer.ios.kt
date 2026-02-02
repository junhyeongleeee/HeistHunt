package com.heisthunt.app.ui.game

import androidx.compose.runtime.Composable
import com.heisthunt.shared.models.PlayerRole
import com.heisthunt.shared.models.Room

@Composable
actual fun GameScreenContainer(
    gameId: String,
    myRole: PlayerRole,
    room: Room?,
    onBack: () -> Unit
) {
    // iOS placeholder - will implement later
    GameScreen(
        gameId = gameId,
        myRole = myRole,
        room = room,
        onBack = onBack,
        uiState = null,
        onRequestCatch = { _, _, _ -> },
        onConfirmCatch = { _, _ -> },
        onRejectCatch = { },
        onDismissCatchRequest = { }
    )
}
