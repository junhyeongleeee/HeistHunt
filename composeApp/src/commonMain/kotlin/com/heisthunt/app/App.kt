package com.heisthunt.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import com.heisthunt.app.di.AppModule
import com.heisthunt.app.ui.auth.LoginScreen
import com.heisthunt.app.ui.auth.RegisterScreen
import com.heisthunt.app.ui.debug.DebugSettingsScreen
import com.heisthunt.app.ui.game.GameScreenContainer
import com.heisthunt.app.ui.game.OperationScreen
import com.heisthunt.app.viewmodel.AuthViewModel
import com.heisthunt.app.viewmodel.RoomViewModel
import kotlin.onSuccess

enum class Screen {
    Login,
    Register,
    Operation,
    Game,
    DebugSettings
}

@Composable
fun App(
    onGoogleLogin: (suspend () -> Boolean)? = null
) {
    val authViewModel = remember { AppModule.provideAuthViewModel() }
    val roomViewModel = remember { AppModule.provideRoomViewModel() }

    val authState by authViewModel.uiState.collectAsState()
    val roomListState by roomViewModel.listState.collectAsState()
    val roomDetailState by roomViewModel.detailState.collectAsState()

    var currentScreen by remember { mutableStateOf(Screen.Login) }

    // Auto-login and game rejoin logic
    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn) {
            // Check for active game
            val gameRepo = AppModule.gameRepository
            val result = gameRepo.getMyActiveGame()

            result.onSuccess { activeGame ->
                if (activeGame != null) {
                    println("✅ Active game found: ${activeGame.gameId}")

                    // Restore game state in RoomViewModel
                    roomViewModel.restoreGameState(
                        gameId = activeGame.gameId,
                        myRole = activeGame.myRole,
                        gameStartTime = activeGame.startTime,
                        escapeDurationSeconds = activeGame.escapeDurationSeconds,
                        totalDurationSeconds = activeGame.totalDurationSeconds
                    )

                    // Navigate to game screen
                    currentScreen = Screen.Game
                } else {
                    println("ℹ️ No active game, going to Operation")
                    currentScreen = Screen.Operation
                }
            }.onFailure { error ->
                println("❌ Failed to check active game: ${error.message}")
                currentScreen = Screen.Operation
            }
        } else {
            currentScreen = Screen.Login
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentScreen) {
                Screen.Login -> {
                    val scope = rememberCoroutineScope()
                    LoginScreen(
                        uiState = authState,
                        onLogin = { email, password ->
                            authViewModel.login(email, password)
                        },
                        onGoogleLogin = {
                            onGoogleLogin?.let {
                                scope.launch {
                                    val success = it()
                                    if (success) {
                                        // Navigate to Operation screen
                                        currentScreen = Screen.Operation
                                    }
                                }
                            }
                        },
                        onNavigateToRegister = {
                            authViewModel.clearError()
                            currentScreen = Screen.Register
                        }
                    )
                }

                Screen.Register -> {
                    RegisterScreen(
                        uiState = authState,
                        onRegister = { email, password, nickname ->
                            authViewModel.register(email, password, nickname)
                        },
                        onNavigateToLogin = {
                            authViewModel.clearError()
                            currentScreen = Screen.Login
                        }
                    )
                }

                Screen.Operation -> {
                    OperationScreen(
                        roomViewModel = roomViewModel,
                        authViewModel = authViewModel,
                        onNavigateToDebug = {
                            currentScreen = Screen.DebugSettings
                        }
                    )
                }

                Screen.Game -> {
                    val gameId = roomDetailState.gameId ?: ""
                    val myRole = roomDetailState.myRole ?: com.heisthunt.shared.models.PlayerRole.THIEF
                    val room = roomDetailState.room

                    println("🎯 App.kt - Screen.Game:")
                    println("   roomDetailState.myRole = ${roomDetailState.myRole}")
                    println("   Using myRole = $myRole")

                    GameScreenContainer(
                        gameId = gameId,
                        myRole = myRole,
                        room = room,
                        startTime = roomDetailState.gameStartTime,
                        escapeDurationSeconds = roomDetailState.escapeDurationSeconds,
                        totalDurationSeconds = roomDetailState.totalDurationSeconds,
                        onBack = {
                            currentScreen = Screen.Operation
                            roomViewModel.clearRoom()
                        }
                    )
                }

                Screen.DebugSettings -> {
                    DebugSettingsScreen(
                        onBack = {
                            currentScreen = Screen.Operation
                        }
                    )
                }
            }
        }
    }
}
