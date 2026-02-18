package com.heisthunt.app.ui.game

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.heisthunt.app.di.AppModule
import com.heisthunt.app.location.LocationService
import com.heisthunt.app.location.RequestLocationPermission
import com.heisthunt.app.utils.HapticFeedback
import com.heisthunt.app.viewmodel.GameViewModel
import com.heisthunt.shared.models.PlayerRole
import com.heisthunt.shared.models.Room

@Composable
actual fun GameScreenContainer(
    gameId: String,
    myRole: PlayerRole,
    room: Room?,
    startTime: kotlinx.datetime.Instant?,
    escapeDurationSeconds: Long,
    totalDurationSeconds: Long,
    onBack: () -> Unit,
    onGameEnded: (com.heisthunt.shared.dto.GameResultResponse) -> Unit
) {
    val context = LocalContext.current
    val locationService = remember { LocationService(context) }
    val hapticFeedback = remember { HapticFeedback(context) }

    // Create unique key to force new ViewModel creation for each game
    val viewModelKey = remember(gameId, myRole, startTime) {
        "$gameId-$myRole-${startTime?.toEpochMilliseconds() ?: System.currentTimeMillis()}"
    }

    val viewModel: GameViewModel = viewModel(key = viewModelKey) {
        println("🎮 [Android] Creating NEW GameViewModel with key: $viewModelKey")
        AppModule.provideGameViewModel(
            gameId = gameId,
            myRole = myRole,
            locationService = locationService,
            hapticFeedback = hapticFeedback,
            startTime = startTime,
            escapeDurationSeconds = escapeDurationSeconds,
            totalDurationSeconds = totalDurationSeconds
        )
    }

    val uiState by viewModel.uiState.collectAsState()

    // Navigate to result screen when game ends
    LaunchedEffect(uiState.isGameEnded, uiState.gameResult) {
        if (uiState.isGameEnded) {
            uiState.gameResult?.let { result ->
                println("🏁 [Android] Game ended, navigating to result screen")
                onGameEnded(result)
            }
        }
    }

    var permissionGranted by remember { mutableStateOf(false) }

    // Request location permission
    RequestLocationPermission(
        onPermissionGranted = {
            permissionGranted = true
            viewModel.startLocationTracking()
        },
        onPermissionDenied = {
            permissionGranted = false
        }
    )

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopLocationTracking()
        }
    }

    // Show error dialog (only for non-GPS/Location errors)
    uiState.error?.takeIf { !it.contains("GPS") && !it.contains("Location") }?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Game Message") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    val myUserId = AppModule.tokenStorage.userId ?: ""
    val myNickname = AppModule.tokenStorage.currentUser.value?.nickname ?: "Player"

    // Render the actual game screen
    // IMPORTANT: Use parameter myRole (from navigation), NOT uiState.myRole
    // uiState.myRole might contain old data if ViewModel wasn't cleared properly
    val actualRole = myRole

    GameScreen(
        gameId = gameId,
        myRole = actualRole,
        room = room,
        onBack = onBack,
        uiState = uiState,
        myUserId = myUserId,
        myNickname = myNickname,
        onRequestCatch = { thiefUserId, policeUserId, policeNickname ->
            viewModel.requestCatch(
                thiefUserId = thiefUserId,
                policeUserId = myUserId,
                policeNickname = myNickname
            )
        },
        onConfirmCatch = { thiefUserId, thiefNickname ->
            viewModel.confirmCatch(
                thiefUserId = myUserId,
                thiefNickname = myNickname
            )
        },
        onRejectCatch = { thiefUserId ->
            viewModel.rejectCatch(myUserId)
        },
        onDismissCatchRequest = {
            viewModel.dismissCatchRequest()
        },
        onLeaveGame = {
            viewModel.leaveGame()
        }
    )
}
