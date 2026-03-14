package com.heisthunt.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import com.heisthunt.app.di.AppModule
import com.heisthunt.app.ui.auth.LoginScreen
import com.heisthunt.app.ui.auth.RegisterScreen
import com.heisthunt.app.ui.auth.SplashScreen
import com.heisthunt.app.ui.debug.DebugSettingsScreen
import com.heisthunt.app.ui.game.GameScreenContainer
import com.heisthunt.app.ui.voicetest.VoiceTestScreenContainer
import com.heisthunt.app.ui.game.OperationScreen
import com.heisthunt.app.ui.game.ResultScreen
import com.heisthunt.app.viewmodel.AuthViewModel
import com.heisthunt.app.viewmodel.RoomViewModel
import kotlin.onSuccess

enum class Screen {
    Splash,
    Login,
    Register,
    Operation,
    Game,
    Result,
    DebugSettings,
    VoiceTest
}

@Composable
fun App(
    onGoogleLogin: (suspend () -> String?)? = null
) {
    val authViewModel = remember { AppModule.provideAuthViewModel() }
    val roomViewModel = remember { AppModule.provideRoomViewModel() }

    val authState by authViewModel.uiState.collectAsState()
    val roomListState by roomViewModel.listState.collectAsState()
    val roomDetailState by roomViewModel.detailState.collectAsState()

    var currentScreen by remember { mutableStateOf(Screen.Splash) }
    var gameResult by remember { mutableStateOf<com.heisthunt.shared.dto.GameResultResponse?>(null) }

    // Force logout observer — triggered by ApiClient when token refresh fails
    LaunchedEffect(Unit) {
        AppModule.forceLogoutFlow.collect {
            println("🔒 [App.kt] Force logout received, clearing auth state")
            authViewModel.forceLogout()
        }
    }

    // Truveri 패턴: 자동로그인 체크 완료 후 목적지를 결정해두고, Splash 최소 시간 후 이동
    var hasCheckedActiveGame by remember { mutableStateOf(false) }
    var splashTargetScreen by remember { mutableStateOf<Screen?>(null) }

    LaunchedEffect(authState.isLoading, authState.isLoggedIn) {
        if (authState.isLoading) return@LaunchedEffect // 아직 체크 중

        if (authState.isLoggedIn && !hasCheckedActiveGame) {
            hasCheckedActiveGame = true
            println("🔍 Checking for active game on login...")

            val gameRepo = AppModule.gameRepository
            val result = gameRepo.getMyActiveGame()
            var target = Screen.Operation

            result.onSuccess { activeGame ->
                if (activeGame != null) {
                    println("✅ Active game found: ${activeGame.gameId}")
                    roomViewModel.restoreGameState(
                        gameId = activeGame.gameId,
                        myRole = activeGame.myRole,
                        gameStartTime = activeGame.startTime,
                        escapeDurationSeconds = activeGame.escapeDurationSeconds,
                        totalDurationSeconds = activeGame.totalDurationSeconds,
                        participants = activeGame.participants
                    )
                    target = Screen.Game
                } else {
                    println("ℹ️ No active game, going to Operation")
                }
            }.onFailure { error ->
                println("❌ Failed to check active game: ${error.message}")
            }

            if (currentScreen == Screen.Splash) splashTargetScreen = target
            else currentScreen = target

        } else if (!authState.isLoggedIn) {
            hasCheckedActiveGame = false
            if (currentScreen == Screen.Splash) splashTargetScreen = Screen.Login
            else currentScreen = Screen.Login
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentScreen) {
                Screen.Splash -> SplashScreen(
                    isAuthReady = splashTargetScreen != null,
                    onNavigate = { currentScreen = splashTargetScreen ?: Screen.Login }
                )

                Screen.Login -> {
                    val scope = rememberCoroutineScope()
                    LoginScreen(
                        uiState = authState,
                        onLogin = { email, password ->
                            authViewModel.login(email, password)
                        },
                        onGoogleLogin = {
                            println("🔵 [App.kt] Google Login button clicked")
                            onGoogleLogin?.let { getIdToken ->
                                scope.launch {
                                    println("🔵 [App.kt] Calling getIdToken()")
                                    val idToken = getIdToken()
                                    println("🔵 [App.kt] getIdToken returned: ${idToken?.take(20)}...")
                                    if (idToken != null) {
                                        println("✅ [App.kt] Got idToken, calling authViewModel.googleLogin()")
                                        authViewModel.googleLogin(idToken)
                                        // Navigation handled by LaunchedEffect(authState.isLoggedIn)
                                    } else {
                                        println("❌ [App.kt] No idToken received (sign-in cancelled or failed)")
                                    }
                                }
                            } ?: println("❌ [App.kt] onGoogleLogin callback is null!")
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
                        },
                        onGameEnded = { result ->
                            println("🏁 [App.kt] Game ended via OperationScreen, navigating to Result screen")
                            gameResult = result
                            currentScreen = Screen.Result
                        }
                    )
                }

                Screen.Game -> {
                    val gameId = roomDetailState.gameId ?: ""
                    val myRole = roomDetailState.myRole
                    val room = roomDetailState.room

                    println("🎯 App.kt - Screen.Game:")
                    println("   roomDetailState.myRole = $myRole")

                    // If myRole is null, show error and return to operation screen
                    if (myRole == null) {
                        println("❌ App.kt - CRITICAL: myRole is null!")
                        LaunchedEffect(Unit) {
                            currentScreen = Screen.Operation
                            roomViewModel.clearRoom()
                        }
                        Text(
                            text = "오류: 역할 정보를 불러올 수 없습니다.",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
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
                            },
                            onGameEnded = { result ->
                                println("🏁 [App.kt] Game ended, navigating to Result screen")
                                gameResult = result
                                currentScreen = Screen.Result
                            }
                        )
                    }
                }

                Screen.Result -> {
                    gameResult?.let { result ->
                        ResultScreen(
                            result = result,
                            myUserId = AppModule.tokenStorage.userId ?: "",
                            onBackToLobby = {
                                println("🔙 [App.kt] Returning to Operation Center from Result screen")
                                gameResult = null
                                currentScreen = Screen.Operation
                                roomViewModel.clearRoom()
                            }
                        )
                    } ?: run {
                        // Fallback if no result available
                        Text(
                            text = "게임 결과를 불러오는 중...",
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Screen.DebugSettings -> {
                    DebugSettingsScreen(
                        onBack = {
                            currentScreen = Screen.Operation
                        },
                        onVoiceTest = {
                            currentScreen = Screen.VoiceTest
                        }
                    )
                }

                Screen.VoiceTest -> {
                    VoiceTestScreenContainer(
                        onBack = { currentScreen = Screen.DebugSettings }
                    )
                }
            }
        }
    }
}
