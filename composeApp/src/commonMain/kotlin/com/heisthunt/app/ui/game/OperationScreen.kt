package com.heisthunt.app.ui.game

import com.heisthunt.app.utils.PlatformBackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heisthunt.app.network.TokenStorage
import com.heisthunt.app.ui.components.QRCodeDisplay
import com.heisthunt.app.viewmodel.RoomViewModel
import com.heisthunt.app.camera.QRScannerView
import com.heisthunt.shared.models.PlayerRole
import com.heisthunt.shared.models.Room

enum class OperationView {
    MAIN, QR_SCANNER, WAITING, GAME
}

@Composable
fun OperationScreen(
    roomViewModel: RoomViewModel,
    authViewModel: com.heisthunt.app.viewmodel.AuthViewModel,
    onNavigateToDebug: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentView by remember { mutableStateOf(OperationView.MAIN) }
    val roomDetailState by roomViewModel.detailState.collectAsState()

    // Navigate to waiting screen when room is created
    LaunchedEffect(roomDetailState.shouldNavigateToWaiting) {
        if (roomDetailState.shouldNavigateToWaiting) {
            currentView = OperationView.WAITING
            roomViewModel.resetNavigationState()
        }
    }

    // Navigate to game screen when game starts
    LaunchedEffect(roomDetailState.shouldNavigateToGame) {
        if (roomDetailState.shouldNavigateToGame) {
            currentView = OperationView.GAME
            roomViewModel.resetGameNavigationState()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020617)) // slate-950
    ) {
        when (currentView) {
            OperationView.MAIN -> MainActionScreen(
                isCreatingRoom = roomDetailState.isLoading,
                error = roomDetailState.error,
                onCreateRoom = {
                    roomViewModel.createRoom("New Operation Room")
                },
                onScanQR = { currentView = OperationView.QR_SCANNER },
                onNavigateToDebug = onNavigateToDebug,
                onLogout = { authViewModel.logout() },
                onClearError = { roomViewModel.clearError() }
            )
            OperationView.QR_SCANNER -> QRScannerScreen(
                roomViewModel = roomViewModel,
                onCancel = {
                    println("🔙 QR Scanner: Cancel clicked")
                    roomViewModel.clearError()
                    currentView = OperationView.MAIN
                }
            )
            OperationView.WAITING -> RoomWaitingScreen(
                room = roomDetailState.room,
                roomViewModel = roomViewModel,
                onBack = {
                    println("🔙 Waiting Room: Back clicked, room=${roomDetailState.room?.id}")
                    roomDetailState.room?.id?.let { roomId ->
                        println("🔙 Leaving room: $roomId")
                        roomViewModel.leaveRoom(roomId)
                    }
                    println("🔙 Setting currentView to MAIN")
                    currentView = OperationView.MAIN
                }
            )
            OperationView.GAME -> GameScreenContainer(
                gameId = roomDetailState.gameId ?: "",
                myRole = roomDetailState.myRole ?: PlayerRole.THIEF,
                room = roomDetailState.room,
                startTime = roomDetailState.gameStartTime,
                escapeDurationSeconds = roomDetailState.escapeDurationSeconds,
                totalDurationSeconds = roomDetailState.totalDurationSeconds,
                onBack = {
                    roomDetailState.room?.id?.let { roomId ->
                        roomViewModel.leaveRoom(roomId)
                    }
                    currentView = OperationView.MAIN
                }
            )
        }
    }
}

@Composable
private fun MainActionScreen(
    isCreatingRoom: Boolean,
    error: String?,
    onCreateRoom: () -> Unit,
    onScanQR: () -> Unit,
    onNavigateToDebug: () -> Unit,
    onLogout: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Error dialog
    if (error != null) {
        AlertDialog(
            onDismissRequest = onClearError,
            title = {
                Text("Error")
            },
            text = {
                Text(error)
            },
            confirmButton = {
                TextButton(onClick = onClearError) {
                    Text("OK")
                }
            }
        )
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(32.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Column(
            modifier = Modifier.padding(top = 32.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HUNT",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = (-1).sp,
                    color = Color(0xFF3B82F6) // blue-500
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "OR",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "HEIST",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = (-1).sp,
                    color = Color(0xFFDC2626) // red-600
                )
            }

            Text(
                text = "OPERATION CENTER",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                color = Color(0xFF64748B), // slate-500
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Create Room Button
            Card(
                onClick = { if (!isCreatingRoom) onCreateRoom() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0F172A) // slate-900
                ),
                border = BorderStroke(1.dp, Color(0xFF1E293B)) // slate-800
            ) {
                Box(
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "LEAD A MISSION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            color = Color(0xFF3B82F6) // blue-500
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "작전 설계하기",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )

                            if (isCreatingRoom) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFF3B82F6),
                                    strokeWidth = 3.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "새로운 게임 방을 만들고 요원을 소집합니다.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            fontStyle = FontStyle.Italic,
                            color = Color(0xFF64748B), // slate-500
                            lineHeight = 20.sp
                        )
                    }

                    // Animated Circle (only when not loading)
                    if (!isCreatingRoom) {
                        val infiniteTransition = rememberInfiniteTransition()
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(3000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            )
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .size(48.dp)
                                .rotate(rotation)
                                .border(
                                    width = 2.dp,
                                    color = Color(0xFF3B82F6).copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .border(
                                    width = 2.dp,
                                    color = Color.Transparent,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }

            // Join via QR Button
            Card(
                onClick = onScanQR,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Box(
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "JOIN OPERATION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            color = Color(0xFFDC2626) // red-600
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "QR 코드로 참여",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A) // slate-900
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "방장의 QR 코드를 스캔하여 즉시 합류합니다.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            fontStyle = FontStyle.Italic,
                            color = Color(0xFF1E293B).copy(alpha = 0.6f),
                            lineHeight = 20.sp
                        )
                    }

                    // QR Icon placeholder
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .size(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⧉",
                            fontSize = 48.sp,
                            color = Color(0xFF0F172A).copy(alpha = 0.3f)
                        )
                    }
                }
            }

            // Debug Settings Button
            TextButton(
                onClick = onNavigateToDebug,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🧪 Debug Settings",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
            }

            // Logout Button
            TextButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🚪 로그아웃",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFDC2626) // red-600
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun QRScannerScreen(
    roomViewModel: RoomViewModel,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scannedCode by remember { mutableStateOf<String?>(null) }

    // Handle Android system back button
    PlatformBackHandler(onBack = onCancel)

    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .background(Color.Black)
    ) {
        // Camera preview with QR scanner
        QRScannerView(
            onQRCodeDetected = { code ->
                if (scannedCode == null) {
                    scannedCode = code
                    roomViewModel.joinRoom(code)
                }
            },
            onError = { error ->
                // Show error via ViewModel
                println("QR Scanner Error: $error")
            },
            modifier = Modifier.fillMaxSize()
        )

        // Scanner overlay UI
        ScannerOverlay(onCancel = onCancel)
    }
}

@Composable
private fun ScannerOverlay(
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Scanner Frame
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(256.dp)
                .border(
                    width = 2.dp,
                    color = Color(0xFF3B82F6), // blue-500
                    shape = RoundedCornerShape(24.dp)
                )
                .background(
                    color = Color(0xFF3B82F6).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            // Corner guides
            val cornerSize = 24.dp
            val cornerWidth = 4.dp

            // Top-left
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset((-2).dp, (-2).dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(cornerSize, cornerWidth)
                        .background(Color.White)
                )
                Box(
                    modifier = Modifier
                        .size(cornerWidth, cornerSize)
                        .background(Color.White)
                )
            }

            // Top-right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(2.dp, (-2).dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(cornerSize, cornerWidth)
                        .background(Color.White)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(cornerWidth, cornerSize)
                        .background(Color.White)
                )
            }

            // Bottom-left
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset((-2).dp, 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(cornerSize, cornerWidth)
                        .background(Color.White)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(cornerWidth, cornerSize)
                        .background(Color.White)
                )
            }

            // Bottom-right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(2.dp, 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(cornerSize, cornerWidth)
                        .background(Color.White)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(cornerWidth, cornerSize)
                        .background(Color.White)
                )
            }

            // Scan line animation
            val infiniteTransition = rememberInfiniteTransition()
            val scanOffset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .offset(y = (256.dp * scanOffset) - 2.dp)
                    .background(Color(0xFF60A5FA)) // blue-400
            )
        }

        // Cancel button
        TextButton(
            onClick = {
                println("🔙 CANCEL button clicked in QRScannerScreen")
                onCancel()
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(32.dp)
        ) {
            Text(
                text = "CANCEL",
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = Color.White
            )
        }

        // Instructions
        Text(
            text = "방장의 QR 코드를 사각형 안에 맞춰주세요",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
            color = Color.White.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RoomWaitingScreen(
    room: Room?,
    roomViewModel: RoomViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // WebSocket connection state
    val wsConnectionState by roomViewModel.wsConnectionState.collectAsState()

    // Connect to WebSocket when room is available
    DisposableEffect(room?.id) {
        if (room?.id != null) {
            println("Connecting to WebSocket for room: ${room.id}")
            roomViewModel.connectToRoom(room.id)
        }
        onDispose {
            println("Disconnecting from WebSocket")
            roomViewModel.disconnectFromRoom()
            // Leave room when screen is disposed (app closed, navigated away, etc.)
            room?.id?.let { roomId ->
                roomViewModel.leaveRoom(roomId)
            }
        }
    }

    // Handle Android system back button
    PlatformBackHandler(onBack = onBack)

    // Show loading if room is null
    if (room == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = Color(0xFF3B82F6)
            )
        }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(
                modifier = Modifier.padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // WebSocket connection status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = when (wsConnectionState) {
                                com.heisthunt.app.network.RoomWebSocketClient.ConnectionState.CONNECTED -> Color(0xFF22C55E) // green
                                com.heisthunt.app.network.RoomWebSocketClient.ConnectionState.CONNECTING -> Color(0xFFFBBF24) // yellow
                                com.heisthunt.app.network.RoomWebSocketClient.ConnectionState.ERROR -> Color(0xFFDC2626) // red
                                else -> Color(0xFF64748B) // gray
                            },
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when (wsConnectionState) {
                        com.heisthunt.app.network.RoomWebSocketClient.ConnectionState.CONNECTED -> "연결됨"
                        com.heisthunt.app.network.RoomWebSocketClient.ConnectionState.CONNECTING -> "연결 중..."
                        com.heisthunt.app.network.RoomWebSocketClient.ConnectionState.ERROR -> "연결 오류"
                        else -> "연결 안됨"
                    },
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8) // slate-400
                )
            }

            Text(
                text = "MISSION BRIEFING",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "OPERATION: ${room.name.uppercase()}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = Color(0xFF64748B) // slate-500
            )
        }

        // QR Code Display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.padding(24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Box(
                    modifier = Modifier
                        .size(192.dp)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    QRCodeDisplay(
                        roomCode = room.code,
                        size = 160.dp
                    )
                }
            }

            Text(
                text = "요원들이 이 QR 코드를 스캔하면\n자동으로 작전에 투입됩니다.",
                fontSize = 14.sp,
                color = Color(0xFF94A3B8), // slate-400
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }

        // Participants and Start Button
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0F172A).copy(alpha = 0.5f) // slate-900/50
            ),
            border = BorderStroke(1.dp, Color(0xFF1E293B)) // slate-800
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Participants count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "투입된 요원",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B) // slate-500
                    )

                    Text(
                        text = "${room.participants.size} / ${room.settings.maxPlayers}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF3B82F6) // blue-500
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Participant avatars (horizontal)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    room.participants.take(8).forEach { participant ->
                        val backgroundColor = when (participant.selectedRole) {
                            PlayerRole.POLICE -> Color(0xFF2563EB) // blue
                            PlayerRole.THIEF -> Color(0xFFDC2626) // red
                            null -> Color(0xFF1E293B) // slate-800
                        }
                        val borderColor = if (participant.isReady) Color(0xFF22C55E) else Color(0xFF334155)

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = backgroundColor,
                                    shape = CircleShape
                                )
                                .border(
                                    width = 2.dp,
                                    color = borderColor,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = participant.nickname.take(1).uppercase(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    if (room.participants.size < room.settings.maxPlayers) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .border(
                                    width = 2.dp,
                                    color = Color(0xFF1E293B),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Role Selection
                val currentUserId = com.heisthunt.app.di.AppModule.tokenStorage.userId
                val myParticipant = room.participants.find { it.userId == currentUserId }
                val selectedRole = myParticipant?.selectedRole
                val isReady = myParticipant?.isReady ?: false

                // Debug logging
                LaunchedEffect(selectedRole) {
                    println("🎮 UI State - currentUserId: $currentUserId")
                    println("🎮 UI State - myParticipant: ${myParticipant?.nickname}")
                    println("🎮 UI State - selectedRole: $selectedRole")
                    println("🎮 UI State - isReady: $isReady")
                    println("🎮 UI State - room.id: ${room.id}")
                }

                Text(
                    text = "역할 선택",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Police button
                    Button(
                        onClick = {
                            println("🔵 Police button clicked! roomId=${room.id}")
                            roomViewModel.selectRole(room.id, PlayerRole.POLICE)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedRole == PlayerRole.POLICE) Color(0xFF2563EB) else Color(0xFF334155),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "경찰",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    // Thief button
                    Button(
                        onClick = {
                            println("🔴 Thief button clicked! roomId=${room.id}")
                            roomViewModel.selectRole(room.id, PlayerRole.THIEF)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedRole == PlayerRole.THIEF) Color(0xFFDC2626) else Color(0xFF334155),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "도둑",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 통합 버튼 (준비/시작)
                val isHost = room.hostId == currentUserId
                val allReady = room.participants.all { it.isReady }
                val allRoleSelected = room.participants.all { it.selectedRole != null }
                val enoughPlayers = room.participants.size >= 2
                val roomDetailState by roomViewModel.detailState.collectAsState()

                // 버튼 상태 결정
                val canStartGame = isHost && isReady && allReady && allRoleSelected && enoughPlayers

                // Debug logging
                LaunchedEffect(isHost, isReady, allReady, allRoleSelected, enoughPlayers, roomDetailState.isLoading) {
                    println("🚀 Button state check:")
                    println("   isHost: $isHost")
                    println("   isReady: $isReady")
                    println("   allReady: $allReady")
                    println("   allRoleSelected: $allRoleSelected")
                    println("   enoughPlayers: $enoughPlayers")
                    println("   canStartGame: $canStartGame")
                    println("   isLoading: ${roomDetailState.isLoading}")
                }

                val buttonText = when {
                    selectedRole == null -> "역할을 선택하세요"
                    !isReady -> "준비"
                    !isHost -> "준비 완료"
                    !enoughPlayers -> "최소 2명 필요"
                    !allRoleSelected -> "모두 역할 선택 필요"
                    !allReady -> "모두 준비 필요"
                    else -> "작전 개시"
                }
                val buttonEnabled = when {
                    selectedRole == null -> false
                    !isReady -> true
                    !isHost -> true // 준비 완료 상태에서 클릭하면 준비 취소
                    canStartGame && !roomDetailState.isLoading -> true
                    else -> false
                }
                val buttonColor = when {
                    canStartGame -> Color(0xFF2563EB) // blue-600 (작전 개시)
                    isReady -> Color(0xFF22C55E) // green-500 (준비 완료)
                    else -> Color(0xFF64748B) // slate-500 (준비)
                }

                Button(
                    onClick = {
                        println("🎯 Button clicked! canStartGame=$canStartGame, buttonText=$buttonText")
                        if (canStartGame) {
                            println("🚀 Starting game for room: ${room.id}")
                            roomViewModel.startGame(room.id)
                        } else {
                            println("✋ Toggling ready for room: ${room.id}")
                            roomViewModel.toggleReady(room.id)
                        }
                    },
                    enabled = buttonEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        disabledContainerColor = Color(0xFF334155)
                    )
                ) {
                    if (roomDetailState.isLoading && canStartGame) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = buttonText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
        }

        // Back button
        IconButton(
            onClick = {
                println("🔙 Back IconButton clicked in RoomWaitingScreen")
                onBack()
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로가기",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
