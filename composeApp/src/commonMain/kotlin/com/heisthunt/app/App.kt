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
import com.heisthunt.app.ui.game.GameScreenContainer
import com.heisthunt.app.ui.game.OperationScreen
import com.heisthunt.app.ui.room.CreateRoomScreen
import com.heisthunt.app.ui.room.RoomListScreen
import com.heisthunt.app.ui.room.RoomLobbyScreen
import com.heisthunt.app.viewmodel.AuthViewModel
import com.heisthunt.app.viewmodel.RoomViewModel

enum class Screen {
    Login,
    Register,
    Operation,
    RoomList,
    CreateRoom,
    RoomDetail,
    Game
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

    // Navigate to operation center when logged in
    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn) {
            currentScreen = Screen.Operation
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
                    OperationScreen(roomViewModel = roomViewModel)
                }

                Screen.RoomList -> {
                    // Navigate to room detail when joined
                    LaunchedEffect(roomDetailState.shouldNavigateToWaiting) {
                        if (roomDetailState.shouldNavigateToWaiting) {
                            currentScreen = Screen.RoomDetail
                            roomViewModel.clearNavigationFlag()
                        }
                    }

                    RoomListScreen(
                        uiState = roomListState,
                        onRefresh = { roomViewModel.loadRooms() },
                        onCreateRoom = {
                            roomViewModel.clearRoom()
                            currentScreen = Screen.CreateRoom
                        },
                        onJoinRoom = { code ->
                            roomViewModel.joinRoom(code)
                        },
                        onRoomClick = { room ->
                            roomViewModel.joinRoom(room.code)
                        }
                    )
                }

                Screen.CreateRoom -> {
                    CreateRoomScreen(
                        uiState = roomDetailState,
                        onCreateRoom = { name, settings ->
                            roomViewModel.createRoom(name, settings)
                        },
                        onBack = {
                            roomViewModel.clearRoom()
                            currentScreen = Screen.RoomList
                        },
                        onRoomCreated = {
                            // Room created, could navigate to room detail
                            currentScreen = Screen.RoomList
                            roomViewModel.loadRooms()
                        }
                    )
                }

                Screen.RoomDetail -> {
                    val room = roomDetailState.room
                    val myUserId = com.heisthunt.app.network.TokenStorage.userId ?: ""
                    val isHost = room?.hostId == myUserId

                    // Navigate to game when game starts
                    LaunchedEffect(roomDetailState.shouldNavigateToGame, roomDetailState.gameId, roomDetailState.myRole) {
                        println("🔍 Navigation check - shouldNavigate: ${roomDetailState.shouldNavigateToGame}, gameId: ${roomDetailState.gameId}, myRole: ${roomDetailState.myRole}")
                        if (roomDetailState.shouldNavigateToGame && roomDetailState.gameId != null && roomDetailState.myRole != null) {
                            println("🎮 Navigating to game screen: gameId=${roomDetailState.gameId}, role=${roomDetailState.myRole}")
                            currentScreen = Screen.Game
                            roomViewModel.clearNavigationFlag()
                        } else {
                            println("❌ Navigation blocked - missing: ${if (!roomDetailState.shouldNavigateToGame) "flag" else if (roomDetailState.gameId == null) "gameId" else "myRole"}")
                        }
                    }

                    RoomLobbyScreen(
                        uiState = roomDetailState,
                        isHost = isHost,
                        myUserId = myUserId,
                        onBack = {
                            roomViewModel.leaveRoom(room?.id ?: "")
                            roomViewModel.clearRoom()
                            currentScreen = Screen.RoomList
                        },
                        onToggleReady = {
                            room?.id?.let { roomViewModel.toggleReady(it) }
                        },
                        onStartGame = {
                            room?.id?.let { roomViewModel.startGame(it) }
                        }
                    )
                }

                Screen.Game -> {
                    val gameId = roomDetailState.gameId ?: ""
                    val myRole = roomDetailState.myRole ?: com.heisthunt.shared.models.PlayerRole.THIEF
                    val room = roomDetailState.room

                    GameScreenContainer(
                        gameId = gameId,
                        myRole = myRole,
                        room = room,
                        startTime = roomDetailState.gameStartTime,
                        escapeDurationSeconds = roomDetailState.escapeDurationSeconds,
                        totalDurationSeconds = roomDetailState.totalDurationSeconds,
                        onBack = {
                            currentScreen = Screen.RoomList
                            roomViewModel.clearRoom()
                        }
                    )
                }
            }
        }
    }
}
