package com.heisthunt.app.ui.voicetest

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heisthunt.app.voice.PeerAudioState
import com.heisthunt.app.voice.PeerConnectionStatus
import com.heisthunt.app.voice.VoiceConnectionState
import com.heisthunt.app.viewmodel.VoiceTestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceTestScreen(
    viewModel: VoiceTestViewModel,
    onBack: () -> Unit
) {
    var roomId by remember { mutableStateOf("1234") }
    var nickname by remember { mutableStateOf("") }

    val isConnected by viewModel.isConnected.collectAsState()
    val myUserId by viewModel.myUserId.collectAsState()
    val peerStates by viewModel.peerStates.collectAsState()
    val voiceState by viewModel.voiceConnectionState.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎤 Voice Channel Test", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isConnected) viewModel.disconnect()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF020617),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF020617)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 연결 설정 카드
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("연결 설정", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        OutlinedTextField(
                            value = roomId,
                            onValueChange = { if (!isConnected) roomId = it },
                            label = { Text("Room ID") },
                            placeholder = { Text("예: 1234") },
                            singleLine = true,
                            enabled = !isConnected,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = Color(0xFF3B82F6),
                                unfocusedLabelColor = Color(0xFF94A3B8),
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFF475569)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = nickname,
                            onValueChange = { if (!isConnected) nickname = it },
                            label = { Text("닉네임 (비워두면 자동 생성)") },
                            singleLine = true,
                            enabled = !isConnected,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = Color(0xFF3B82F6),
                                unfocusedLabelColor = Color(0xFF94A3B8),
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFF475569)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                if (isConnected) viewModel.disconnect()
                                else viewModel.connect(roomId, nickname)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isConnected) Color(0xFFEF4444) else Color(0xFF3B82F6)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                if (isConnected) "연결 해제" else "방 입장",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 내 상태 + 마이크 버튼
            if (isConnected) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("내 상태", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Room: $roomId", fontSize = 14.sp, color = Color(0xFF94A3B8))
                                    Text("ID: ${myUserId.take(12)}...", fontSize = 12.sp, color = Color(0xFF64748B))
                                    Text(
                                        "음성: ${voiceState.displayName()}",
                                        fontSize = 14.sp,
                                        color = voiceState.statusColor()
                                    )
                                    if (!isMuted && isSpeaking) {
                                        Text(
                                            "🎙️ 말하는 중...",
                                            fontSize = 13.sp,
                                            color = Color(0xFF22C55E),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                // 마이크 버튼 + 파동 애니메이션
                                MicButton(
                                    isMuted = isMuted,
                                    isSpeaking = isSpeaking && !isMuted,
                                    onToggle = { viewModel.toggleMute() }
                                )
                            }
                        }
                    }
                }

                // 피어 목록
                item {
                    Text(
                        "피어 (${peerStates.size}명)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (peerStates.isEmpty()) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "다른 기기에서 같은 Room ID로 입장하세요",
                                    fontSize = 14.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                items(peerStates.values.toList()) { peer ->
                    PeerCard(peer = peer)
                }
            }
        }
    }
}

@Composable
private fun MicButton(
    isMuted: Boolean,
    isSpeaking: Boolean,
    onToggle: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic")

    // 파동 scale: 1.0 → 1.6
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleScale"
    )
    // 파동 투명도: 0.5 → 0
    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleAlpha"
    )
    // 버튼 펄스 scale
    val buttonScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "buttonScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(80.dp)
    ) {
        // 파동 원 (말하는 중일 때만)
        if (isSpeaking) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer {
                        scaleX = rippleScale
                        scaleY = rippleScale
                        alpha = rippleAlpha
                    }
                    .background(Color(0xFF22C55E), CircleShape)
            )
        }
        // 마이크 버튼
        Button(
            onClick = onToggle,
            colors = ButtonDefaults.buttonColors(
                containerColor = when {
                    isMuted -> Color(0xFFEF4444)
                    isSpeaking -> Color(0xFF16A34A)
                    else -> Color(0xFF22C55E)
                }
            ),
            shape = CircleShape,
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    if (isSpeaking) {
                        scaleX = buttonScale
                        scaleY = buttonScale
                    }
                },
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(if (isMuted) "🔇" else "🎙️", fontSize = 24.sp)
        }
    }
}

@Composable
private fun PeerCard(peer: PeerAudioState) {
    val infiniteTransition = rememberInfiniteTransition(label = "peer_${peer.userId}")

    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "peerRipple"
    )
    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "peerRippleAlpha"
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (peer.isSpeaking) Color(0xFF22C55E).copy(alpha = 0.15f)
                             else Color(0xFF1E293B)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 아바타 + 파동
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                    if (peer.isSpeaking) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .graphicsLayer {
                                    scaleX = rippleScale
                                    scaleY = rippleScale
                                    alpha = rippleAlpha
                                }
                                .background(Color(0xFF22C55E), CircleShape)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = peer.connectionStatus.statusColor().copy(alpha = 0.2f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(peer.connectionStatus.statusIcon(), fontSize = 18.sp)
                    }
                }

                Column {
                    Text(
                        peer.nickname.ifBlank { peer.userId.take(8) },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Text(
                        if (peer.isSpeaking) "🎙️ 말하는 중..."
                        else peer.connectionStatus.displayName(),
                        fontSize = 12.sp,
                        color = if (peer.isSpeaking) Color(0xFF22C55E)
                                else peer.connectionStatus.statusColor(),
                        fontWeight = if (peer.isSpeaking) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            if (peer.isRemoteMuted) Text("🔇", fontSize = 20.sp)
        }
    }
}

private fun VoiceConnectionState.displayName() = when (this) {
    VoiceConnectionState.IDLE -> "대기 중"
    VoiceConnectionState.CONNECTING -> "연결 중..."
    VoiceConnectionState.ACTIVE -> "활성"
}

private fun VoiceConnectionState.statusColor() = when (this) {
    VoiceConnectionState.IDLE -> Color(0xFF64748B)
    VoiceConnectionState.CONNECTING -> Color(0xFFF59E0B)
    VoiceConnectionState.ACTIVE -> Color(0xFF22C55E)
}

private fun PeerConnectionStatus.displayName() = when (this) {
    PeerConnectionStatus.DISCONNECTED -> "연결 끊김"
    PeerConnectionStatus.CONNECTING -> "연결 중..."
    PeerConnectionStatus.CONNECTED -> "연결됨"
}

private fun PeerConnectionStatus.statusColor() = when (this) {
    PeerConnectionStatus.DISCONNECTED -> Color(0xFFEF4444)
    PeerConnectionStatus.CONNECTING -> Color(0xFFF59E0B)
    PeerConnectionStatus.CONNECTED -> Color(0xFF22C55E)
}

private fun PeerConnectionStatus.statusIcon() = when (this) {
    PeerConnectionStatus.DISCONNECTED -> "✗"
    PeerConnectionStatus.CONNECTING -> "⟳"
    PeerConnectionStatus.CONNECTED -> "✓"
}
