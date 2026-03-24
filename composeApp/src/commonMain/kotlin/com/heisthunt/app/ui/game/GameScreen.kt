package com.heisthunt.app.ui.game

import com.heisthunt.app.utils.PlatformBackHandler
import com.heisthunt.app.utils.formatDecimal
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heisthunt.app.viewmodel.GameUiState
import com.heisthunt.app.viewmodel.ThiefBoundaryViolationState
import com.heisthunt.app.voice.VoiceChannelManager
import com.heisthunt.shared.models.Participant
import com.heisthunt.shared.models.PlayerLocation
import com.heisthunt.shared.models.PlayerRole
import com.heisthunt.shared.models.Room

@Composable
fun GameScreen(
    gameId: String,
    myRole: PlayerRole,
    room: Room?,
    onBack: () -> Unit,
    uiState: GameUiState?,
    myUserId: String = "",
    myNickname: String = "Player",
    voiceChannelManager: VoiceChannelManager? = null,
    teammates: List<Participant> = emptyList(),
    onRequestCatch: (thiefUserId: String, policeUserId: String, policeNickname: String) -> Unit,
    onConfirmCatch: (thiefUserId: String, thiefNickname: String) -> Unit,
    onRejectCatch: (thiefUserId: String) -> Unit,
    onDismissCatchRequest: () -> Unit,
    onLeaveGame: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    println("🎨 GameScreen rendering: myRole=$myRole, uiState.myRole=${uiState?.myRole}")

    // Use real data from local timer (hybrid approach)
    val timeLeft = uiState?.localRemainingSeconds?.toInt() ?: 300
    val escapeTimeLeft = uiState?.localEscapeRemainingSeconds?.toInt() ?: 300
    val totalGameTime = uiState?.totalDurationSeconds?.toInt() ?: 1200
    val myLoc = uiState?.myLocation
    val centerLoc = uiState?.policeJailLocation
    val distanceFromCenter = if (myLoc != null && centerLoc != null) {
        com.heisthunt.shared.utils.GeoUtils.calculateDistance(
            myLoc.latitude, myLoc.longitude,
            centerLoc.latitude, centerLoc.longitude
        ).toInt()
    } else 0
    val maxRadius = 500
    val gamePhase = uiState?.currentPhase ?: "ESCAPE"
    val remainingThieves = uiState?.gameStatus?.remainingThieves ?: 3
    val totalThieves = uiState?.gameStatus?.totalThieves ?: 5

    // Leave game confirmation dialog state
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showTeamPanel by remember { mutableStateOf(false) }
    var showPlayerListPanel by remember { mutableStateOf(false) }

    // BackHandler - show confirmation dialog instead of leaving directly
    PlatformBackHandler(onBack = { showLeaveDialog = true })

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .background(Color(0xFF020617)) // slate-950
        ) {
            // Status Bar (상단)
            StatusBar(
                role = myRole,
                timeLeft = timeLeft,
                escapeTimeLeft = escapeTimeLeft,
                totalGameTime = totalGameTime,
                gamePhase = gamePhase,
                distanceFromCenter = distanceFromCenter,
                maxRadius = maxRadius,
                escapeDurationSeconds = uiState?.escapeDurationSeconds ?: 300L,
                onLeaveClick = { showLeaveDialog = true }
            )

            // Map Section (지도)
            Box(modifier = Modifier.weight(1f)) {
                // 역할과 페이즈에 따라 표시할 플레이어 위치 필터링
                // - ESCAPE + 도둑: 모든 유저 위치 (스펙: 도둑은 모든 위치 확인 가능)
                // - ESCAPE + 경찰: 경찰끼리만 (같은 팀끼리 항상 공유)
                // - CHASE + 도둑: 도둑끼리만 (스펙: 도둑은 도둑만 확인 가능)
                // - CHASE + 경찰: 경찰끼리만 (스펙: 경찰은 경찰만 확인 가능)
                val visiblePlayerLocations = (uiState?.playerLocations ?: emptyList()).filter { player ->
                    when {
                        myRole == PlayerRole.THIEF && gamePhase == "ESCAPE" -> true
                        myRole == PlayerRole.THIEF -> player.role == PlayerRole.THIEF
                        else -> player.role == PlayerRole.POLICE
                    }
                }
                println("🗺️ [GameScreen] visiblePlayerLocations: ${visiblePlayerLocations.size} / ${uiState?.playerLocations?.size ?: 0} (role=$myRole, phase=$gamePhase)")

                GameMapView(
                    myLocation = uiState?.myLocation,
                    playerLocations = visiblePlayerLocations,
                    myRole = myRole,
                    safeRadiusMeters = maxRadius.toDouble(),
                    gameCenterLocation = uiState?.policeJailLocation,
                    disconnectedPlayerIds = uiState?.disconnectedPlayerIds ?: emptySet(),
                    modifier = Modifier.fillMaxSize()
                )

                // GPS/Location 에러는 표시하지 않음 (실내에서도 게임 가능)

                // ESCAPE phase: Police violation alert (thieves only)
                if (myRole == PlayerRole.THIEF &&
                    uiState?.policeViolation != null &&
                    gamePhase == "ESCAPE") {
                    PoliceViolationAlert(
                        distanceFromJail = uiState.policeViolation.distanceFromJail,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }

                // CHASE phase: Proximity alert (both roles)
                if (uiState?.nearbyEnemies != null && gamePhase == "CHASE") {
                    ProximityAlert(
                        myRole = myRole,
                        enemyCount = uiState.nearbyEnemies.count,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }

                // Thief boundary violation alert (all phases)
                if (uiState?.thiefBoundaryViolation != null) {
                    ThiefBoundaryViolationAlert(
                        myUserId = myUserId,
                        myRole = myRole,
                        violation = uiState.thiefBoundaryViolation,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }

                // Legacy: Police thief detection alert (kept for backward compatibility)
                // This is now replaced by ProximityAlert but keeping it for now
                /*
                if (myRole == PlayerRole.POLICE && uiState?.nearbyThieves?.isNotEmpty() == true) {
                    CatchDetectionAlert(
                        nearbyThievesCount = uiState.nearbyThieves.size,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
                */

                // Top-right overlay buttons
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Team voice channel button
                    if (voiceChannelManager != null) {
                        FloatingActionButton(
                            onClick = { showTeamPanel = !showTeamPanel; showPlayerListPanel = false },
                            modifier = Modifier.size(44.dp),
                            containerColor = Color(0xFF0F172A).copy(alpha = 0.9f),
                            elevation = FloatingActionButtonDefaults.elevation(4.dp)
                        ) {
                            Text(
                                text = if (myRole == PlayerRole.POLICE) "🚔" else "💰",
                                fontSize = 18.sp
                            )
                        }
                    }
                    // Player list button
                    FloatingActionButton(
                        onClick = { showPlayerListPanel = !showPlayerListPanel; showTeamPanel = false },
                        modifier = Modifier.size(44.dp),
                        containerColor = Color(0xFF0F172A).copy(alpha = 0.9f),
                        elevation = FloatingActionButtonDefaults.elevation(4.dp)
                    ) {
                        Text(text = "👥", fontSize = 18.sp)
                    }
                }
            }

            // Action Footer (하단)
            ActionFooter(
                role = myRole,
                gamePhase = gamePhase,
                remainingThieves = remainingThieves,
                totalThieves = totalThieves,
                nearbyThieves = uiState?.catchableThieves ?: emptyList(),
                isCaught = uiState?.isCaught == true,
                escapeDurationSeconds = uiState?.escapeDurationSeconds ?: 300L,
                myPoliceId = myUserId,
                myPoliceNickname = myNickname,
                onRequestCatch = onRequestCatch
            )
        }

        // Thief: Catch request dialog
        uiState?.catchRequest?.let { request ->
            CatchRequestDialog(
                policeNickname = request.policeNickname,
                onConfirm = { onConfirmCatch(request.thiefUserId, myNickname) },
                onReject = { onRejectCatch(request.thiefUserId) },
                onDismiss = onDismissCatchRequest
            )
        }

        // Leave game confirmation dialog
        if (showLeaveDialog) {
            LeaveGameDialog(
                onConfirm = {
                    showLeaveDialog = false
                    onLeaveGame()
                    onBack()
                },
                onDismiss = { showLeaveDialog = false }
            )
        }

        // Team voice panel (slides in from right)
        if (voiceChannelManager != null) {
            TeamVoicePanel(
                visible = showTeamPanel,
                voiceChannelManager = voiceChannelManager,
                myUserId = myUserId,
                teammates = teammates,
                myRole = myRole,
                onDismiss = { showTeamPanel = false },
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        // Player list panel (slides in from right)
        PlayerListPanel(
            visible = showPlayerListPanel,
            participants = uiState?.allParticipants ?: emptyList(),
            currentPhase = gamePhase,
            onDismiss = { showPlayerListPanel = false },
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun StatusBar(
    role: PlayerRole,
    timeLeft: Int,
    escapeTimeLeft: Int,
    totalGameTime: Int,
    gamePhase: String,
    distanceFromCenter: Int,
    maxRadius: Int,
    escapeDurationSeconds: Long,
    onLeaveClick: () -> Unit
) {
    println("🎨 StatusBar rendering: role=$role")

    val backgroundColor = if (role == PlayerRole.THIEF) {
        Color(0xFF450A0A) // red-950/20
    } else {
        Color(0xFF172554) // blue-950/20
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .border(1.dp, Color(0xFF0F172A), RoundedCornerShape(0.dp))
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .padding(top = 32.dp)
    ) {
        // Leave button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            IconButton(
                onClick = {
                    println("🔘 Leave button clicked!")
                    onLeaveClick()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(48.dp) // Increase touch area for iOS
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "게임 나가기",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        // Timer and Radius Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Mission Time - 페이즈별로 다른 타이머 표시
            Column {
                if (gamePhase == "ESCAPE") {
                    // ESCAPE 페이즈: 도망 시간 타이머
                    Text(
                        text = "ESCAPE TIME",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        color = Color(0xFF22C55E) // green-500
                    )

                    val minutes = escapeTimeLeft / 60
                    val seconds = escapeTimeLeft % 60
                    val timeColor = if (escapeTimeLeft < 60) Color(0xFFFBBF24) else Color(0xFF22C55E) // yellow or green

                    Text(
                        text = "$minutes:${seconds.toString().padStart(2, '0')}",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        color = timeColor,
                        modifier = if (escapeTimeLeft < 60) {
                            Modifier.alpha(
                                rememberInfiniteTransition().animateFloat(
                                    initialValue = 0.5f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(500),
                                        repeatMode = RepeatMode.Reverse
                                    )
                                ).value
                            )
                        } else Modifier
                    )
                } else {
                    // CHASE 페이즈: 전체 게임 시간 타이머
                    Text(
                        text = "GAME TIME",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        color = Color(0xFF64748B) // slate-500
                    )

                    val minutes = timeLeft / 60
                    val seconds = timeLeft % 60
                    val timeColor = if (timeLeft < 60) Color(0xFFEF4444) else Color.White

                    Text(
                        text = "$minutes:${seconds.toString().padStart(2, '0')}",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        color = timeColor,
                        modifier = if (timeLeft < 60) {
                            Modifier.alpha(
                                rememberInfiniteTransition().animateFloat(
                                    initialValue = 0.5f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(500),
                                        repeatMode = RepeatMode.Reverse
                                    )
                                ).value
                            )
                        } else Modifier
                    )
                }
            }

            // Safe Radius
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "SAFE RADIUS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = Color(0xFF64748B)
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val radiusColor = if (distanceFromCenter > maxRadius - 50) {
                        Color(0xFFEF4444) // red-500
                    } else {
                        Color(0xFF22C55E) // green-500
                    }

                    Text(
                        text = "${distanceFromCenter}m",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = radiusColor
                    )
                    Text(
                        text = "/ ${maxRadius}m",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress Indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Color(0xFF1E293B), RoundedCornerShape(2.dp))
        ) {
            val progressColor = if (role == PlayerRole.THIEF) {
                Color(0xFFDC2626) // red-600
            } else {
                Color(0xFF3B82F6) // blue-500
            }

            // Use escape timer during ESCAPE phase, total timer during CHASE
            val progress = if (gamePhase == "ESCAPE") {
                val escapeDurationFloat = escapeDurationSeconds.toFloat()
                (escapeTimeLeft.toFloat() / escapeDurationFloat).coerceIn(0f, 1f)
            } else {
                (timeLeft.toFloat() / totalGameTime.toFloat()).coerceIn(0f, 1f)
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(progressColor, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
private fun MapErrorOverlay(
    errorMessage: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x80000000)) // semi-transparent black
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(top = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xE6DC2626) // red-600/90
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚠️",
                    fontSize = 20.sp
                )
                Text(
                    text = errorMessage,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun CatchDetectionAlert(
    nearbyThievesCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .padding(top = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xE62563EB) // blue-600/90
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .alpha(
                    rememberInfiniteTransition().animateFloat(
                        initialValue = 0.7f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(500),
                            repeatMode = RepeatMode.Reverse
                        )
                    ).value
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🎯",
                fontSize = 24.sp
            )
            Column {
                Text(
                    text = "도둑 감지!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "반경 5m 이내에 도둑 ${nearbyThievesCount}명",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun PoliceViolationAlert(
    distanceFromJail: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .padding(top = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xE6DC2626) // red-600/90
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .alpha(
                    rememberInfiniteTransition().animateFloat(
                        initialValue = 0.7f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(500),
                            repeatMode = RepeatMode.Reverse
                        )
                    ).value
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚠️",
                fontSize = 24.sp
            )
            Column {
                Text(
                    text = "경찰이 이탈했습니다!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "감옥에서 ${distanceFromJail.toInt()}m 이탈",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun ThiefBoundaryViolationAlert(
    myUserId: String,
    myRole: PlayerRole,
    violation: ThiefBoundaryViolationState,
    modifier: Modifier = Modifier
) {
    val isMyViolation = violation.thiefUserId == myUserId
    val bgColor = if (violation.isSentToJail) Color(0xE6991B1B) else Color(0xE6D97706) // red-800 or amber-600
    val title = when {
        violation.isSentToJail && isMyViolation -> "⛓️ 감옥 처리!"
        violation.isSentToJail -> "⛓️ 도둑 감옥 처리!"
        isMyViolation -> "🚨 반경 이탈!"
        else -> "🚨 도둑 반경 이탈!"
    }
    val subtitle = when {
        violation.isSentToJail && isMyViolation -> "부정행위로 감옥으로 이동합니다"
        violation.isSentToJail -> "${violation.thiefNickname} 부정행위로 감옥 처리됨"
        isMyViolation -> "게임 반경을 벗어났습니다! 경고 ${violation.warningCount}/3"
        else -> "${violation.thiefNickname} 경고 ${violation.warningCount}/3"
    }
    Card(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .padding(top = 16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .alpha(
                    rememberInfiniteTransition().animateFloat(
                        initialValue = 0.7f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(500),
                            repeatMode = RepeatMode.Reverse
                        )
                    ).value
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = if (violation.isSentToJail) "⛓️" else "⚠️", fontSize = 24.sp)
            Column {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text(text = subtitle, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.9f))
            }
        }
    }
}

@Composable
private fun ProximityAlert(
    myRole: PlayerRole,
    enemyCount: Int,
    modifier: Modifier = Modifier
) {
    val (message, icon, color) = when (myRole) {
        PlayerRole.THIEF -> Triple("경찰 감지!", "🚨", Color(0xE6DC2626)) // red-600/90
        PlayerRole.POLICE -> Triple("도둑 감지!", "🎯", Color(0xE62563EB)) // blue-600/90
    }

    Card(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .padding(top = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .alpha(
                    rememberInfiniteTransition().animateFloat(
                        initialValue = 0.7f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(500),
                            repeatMode = RepeatMode.Reverse
                        )
                    ).value
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = 24.sp
            )
            Column {
                Text(
                    text = message,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "반경 5m 이내에 ${enemyCount}명",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun CatchRequestDialog(
    policeNickname: String,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🚨",
                    fontSize = 40.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "체포 요청",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }
        },
        text = {
            Text(
                text = "경찰 '$policeNickname'이(가) 당신을 체포했다고 주장합니다.\n\n정말 잡혔습니까?",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC2626) // red-600
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "인정 (감옥으로 이동)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onReject,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Color(0xFF64748B))
            ) {
                Text(
                    text = "거부",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF64748B)
                )
            }
        },
        containerColor = Color(0xFF0F172A),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

@Composable
private fun CatchTargetDialog(
    catchableThieves: List<Participant>,
    onSelectThief: (thiefUserId: String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🎯",
                    fontSize = 40.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "체포 대상 선택",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "누구를 체포하시겠습니까?",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                catchableThieves.forEach { thief ->
                    Button(
                        onClick = { onSelectThief(thief.userId) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB) // blue-600
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = thief.nickname,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Color(0xFF64748B))
            ) {
                Text(
                    text = "취소",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF64748B)
                )
            }
        },
        containerColor = Color(0xFF0F172A),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

// Legacy MapSection - kept for reference, can be removed later
@Composable
private fun MapSection_Legacy(
    role: PlayerRole,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // slate-900
    ) {
        // TODO: Replace with actual Google Maps
        // Simulated map background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.4f)
                .background(Color(0xFF1E293B))
        )

        // Radius guideline (dashed circle)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(300.dp)
                .border(
                    width = 2.dp,
                    color = Color(0x4DEF4444), // red-500/30
                    shape = CircleShape
                )
                .alpha(
                    rememberInfiniteTransition().animateFloat(
                        initialValue = 0.3f,
                        targetValue = 0.7f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000),
                            repeatMode = RepeatMode.Reverse
                        )
                    ).value
                )
        )

        // Player markers
        // 1. Me (center)
        Box(
            modifier = Modifier.align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            val markerColor = if (role == PlayerRole.THIEF) {
                Color(0xFFDC2626) // red-600
            } else {
                Color(0xFF3B82F6) // blue-500
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(markerColor, CircleShape)
                    .border(4.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ME",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }

        // 2. Teammate (simulated position)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 80.dp, y = (-60).dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val markerColor = if (role == PlayerRole.THIEF) {
                    Color(0xFFDC2626)
                } else {
                    Color(0xFF3B82F6)
                }

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .alpha(0.8f)
                        .background(markerColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.White, CircleShape)
                    )
                }

                Text(
                    text = "AGENT_K",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .background(Color(0x80000000), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }

        // Game message alert (toast)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .fillMaxWidth(0.9f)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xE6DC2626) // red-600/90
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🚨",
                        fontSize = 20.sp
                    )
                    Text(
                        text = "[ALERT] 도둑 'Hacker_9'가 반경을 이탈하여 탈락했습니다!",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionFooter(
    role: PlayerRole,
    gamePhase: String,
    remainingThieves: Int,
    totalThieves: Int,
    nearbyThieves: List<Participant>,
    isCaught: Boolean,
    escapeDurationSeconds: Long,
    myPoliceId: String,
    myPoliceNickname: String,
    onRequestCatch: (thiefUserId: String, policeUserId: String, policeNickname: String) -> Unit
) {
    var showCatchDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF020617)) // slate-950
            .border(1.dp, Color(0xFF0F172A), RoundedCornerShape(0.dp))
            .padding(32.dp)
    ) {
        if (role == PlayerRole.POLICE) {
            // Police UI: Capture button
            val isEscapePhase = gamePhase == "ESCAPE"
            val canCatch = !isEscapePhase && nearbyThieves.isNotEmpty()

            Button(
                onClick = {
                    if (canCatch) {
                        showCatchDialog = true
                    }
                },
                enabled = canCatch,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canCatch) Color(0xFF2563EB) else Color(0xFF334155),
                    disabledContainerColor = Color(0xFF1E293B)
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = when {
                            isEscapePhase -> "ESCAPE PHASE"
                            nearbyThieves.isNotEmpty() -> "I GOT 'EM!"
                            else -> "NO TARGETS"
                        },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = (-1).sp,
                        color = if (canCatch) Color.White else Color(0xFF64748B)
                    )
                    Text(
                        text = when {
                            isEscapePhase -> "도둑이 도망치는 중..."
                            nearbyThieves.isNotEmpty() -> "체포 가능한 도둑 ${nearbyThieves.size}명"
                            else -> "체포 가능한 도둑이 없습니다"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = if (canCatch) Color.White.copy(alpha = 0.7f) else Color(0xFF475569),
                        modifier = Modifier.alpha(0.7f)
                    )
                }
            }

            // Catch target selection dialog
            if (showCatchDialog) {
                CatchTargetDialog(
                    catchableThieves = nearbyThieves,
                    onSelectThief = { thiefUserId ->
                        onRequestCatch(thiefUserId, myPoliceId, myPoliceNickname)
                        showCatchDialog = false
                    },
                    onDismiss = { showCatchDialog = false }
                )
            }
        } else {
            // Thief UI: Survival status
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Remaining Thieves
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF0F172A) // slate-900
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0x4D7F1D1D)) // red-900/30
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "REMAINING THIEVES",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = "$remainingThieves / $totalThieves",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFEF4444) // red-500
                            )
                        }
                    }

                    // Status
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0x4D1E3A8A)) // blue-900/30
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "STATUS",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = if (isCaught) "CAUGHT" else "EVADING",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                fontStyle = FontStyle.Italic,
                                color = if (isCaught) Color(0xFFEF4444) else Color(0xFF22C55E)
                            )
                        }
                    }
                }

                // Phase message
                when (gamePhase) {
                    "ESCAPE" -> {
                        Text(
                            text = "※ 도망 시간: ${escapeDurationSeconds / 60}분간 숨을 수 있습니다",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF22C55E), // green-500
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(
                                    rememberInfiniteTransition().animateFloat(
                                        initialValue = 0.5f,
                                        targetValue = 1f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(1000),
                                            repeatMode = RepeatMode.Reverse
                                        )
                                    ).value
                                )
                        )
                    }
                    "CHASE" -> {
                        Text(
                            text = "⚠️ 추격 시작! 경찰이 당신을 쫓고 있습니다!",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444), // red-500
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(
                                    rememberInfiniteTransition().animateFloat(
                                        initialValue = 0.5f,
                                        targetValue = 1f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(500),
                                            repeatMode = RepeatMode.Reverse
                                        )
                                    ).value
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaveGameDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "⚠️",
                    fontSize = 40.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "게임 나가기",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }
        },
        text = {
            Text(
                text = "정말로 게임을 나가시겠습니까?\n\n게임이 진행 중이며, 나가면 다시 돌아올 수 없습니다.",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC2626) // red-600
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "나가기",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Color(0xFF64748B))
            ) {
                Text(
                    text = "취소",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF64748B)
                )
            }
        },
        containerColor = Color(0xFF0F172A),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}
