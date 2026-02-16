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
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val locationService = remember { LocationService(context) }

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
            startTime = startTime,
            escapeDurationSeconds = escapeDurationSeconds,
            totalDurationSeconds = totalDurationSeconds
        )
    }

    val uiState by viewModel.uiState.collectAsState()

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

    // TODO: Get actual user ID and nickname from auth state
    val myUserId = "USER_${gameId.take(8)}" // Placeholder
    val myNickname = "Player" // Placeholder

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
