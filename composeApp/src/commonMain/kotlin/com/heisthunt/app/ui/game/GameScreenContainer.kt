package com.heisthunt.app.ui.game

import androidx.compose.runtime.*
import com.heisthunt.shared.models.PlayerRole
import com.heisthunt.shared.models.Room

@Composable
expect fun GameScreenContainer(
    gameId: String,
    myRole: PlayerRole,
    room: Room?,
    startTime: kotlinx.datetime.Instant?,
    escapeDurationSeconds: Long,
    totalDurationSeconds: Long,
    onBack: () -> Unit
)
