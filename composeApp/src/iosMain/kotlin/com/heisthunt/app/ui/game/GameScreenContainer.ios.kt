package com.heisthunt.app.ui.game

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import com.heisthunt.app.di.AppModule
import com.heisthunt.app.location.LocationService
import com.heisthunt.app.viewmodel.GameViewModel
import com.heisthunt.shared.models.PlayerRole
import com.heisthunt.shared.models.Room
import kotlinx.datetime.Clock

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
    val locationService = remember { LocationService() }

    // Create unique key to force new ViewModel creation for each game
    // IMPORTANT: Don't cache ViewModel - create new one for each game
    val viewModelKey = remember(gameId, myRole, startTime) {
        "$gameId-$myRole-${startTime?.toEpochMilliseconds() ?: Clock.System.now().toEpochMilliseconds()}"
    }

    val viewModel = remember(viewModelKey) {
        println("═══════════════════════════════════════════")
        println("🎮 [iOS] Creating NEW GameViewModel")
        println("═══════════════════════════════════════════")
        println("  viewModelKey: $viewModelKey")
        println("  gameId: $gameId")
        println("  myRole: $myRole (${myRole.name})")
        println("  startTime: $startTime")
        println("  startTime is null: ${startTime == null}")
        println("  escapeDurationSeconds: $escapeDurationSeconds")
        println("  totalDurationSeconds: $totalDurationSeconds")
        println("═══════════════════════════════════════════")

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

    LaunchedEffect(uiState) {
        println("🎮 [iOS] UI State changed:")
        println("  myRole: ${uiState.myRole}")
        println("  currentPhase: ${uiState.currentPhase}")
        println("  localRemainingSeconds: ${uiState.localRemainingSeconds}")
        println("  localEscapeRemainingSeconds: ${uiState.localEscapeRemainingSeconds}")
        println("  startTime: ${uiState.startTime}")
    }

    var permissionGranted by remember { mutableStateOf(false) }

    // iOS location permission handling
    LaunchedEffect(Unit) {
        // iOS will automatically request permission when location services are accessed
        // Just start tracking, CLLocationManager will handle permission dialog
        permissionGranted = true
        viewModel.startLocationTracking()
    }

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

    println("🎨 [iOS] Rendering GameScreen:")
    println("  actualRole: $actualRole (${actualRole.name})")
    println("  uiState.myRole: ${uiState.myRole?.name}")
    println("  uiState.currentPhase: ${uiState.currentPhase}")
    println("  uiState.localRemainingSeconds: ${uiState.localRemainingSeconds}")
    println("  uiState.localEscapeRemainingSeconds: ${uiState.localEscapeRemainingSeconds}")
    println("  uiState.startTime: ${uiState.startTime}")

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
