package com.heisthunt.app.ui.game

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heisthunt.app.voice.PeerAudioState
import com.heisthunt.app.voice.PeerConnectionStatus
import com.heisthunt.app.voice.VoiceChannelManager
import com.heisthunt.app.voice.VoiceConnectionState
import com.heisthunt.shared.models.Participant
import com.heisthunt.shared.models.PlayerRole

@Composable
fun TeamVoicePanel(
    visible: Boolean,
    voiceChannelManager: VoiceChannelManager,
    myUserId: String,
    teammates: List<Participant>,
    myRole: PlayerRole,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connectionState by voiceChannelManager.connectionState.collectAsState()
    val peerStates by voiceChannelManager.peerStates.collectAsState()
    val isMuted by voiceChannelManager.isMuted.collectAsState()

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(250)
        ) + fadeOut(animationSpec = tween(250)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(280.dp)
                .background(Color(0xFF0F172A))
                .navigationBarsPadding()
                .padding(top = 56.dp, bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (myRole == PlayerRole.POLICE) "🚔 경찰 채널" else "💰 도둑 채널",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E8F0)
                        )
                        Text(
                            text = when (connectionState) {
                                VoiceConnectionState.ACTIVE -> "● 활성"
                                VoiceConnectionState.CONNECTING -> "◌ 연결 중..."
                                VoiceConnectionState.IDLE -> "○ 대기 중"
                            },
                            fontSize = 12.sp,
                            color = when (connectionState) {
                                VoiceConnectionState.ACTIVE -> Color(0xFF22C55E)
                                VoiceConnectionState.CONNECTING -> Color(0xFFF59E0B)
                                VoiceConnectionState.IDLE -> Color(0xFF64748B)
                            }
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Text("✕", fontSize = 18.sp, color = Color(0xFF64748B))
                    }
                }

                HorizontalDivider(color = Color(0xFF1E293B))

                // Teammates list
                Text(
                    text = "팀원",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 1.sp
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(teammates.filter { it.userId != myUserId }) { teammate ->
                        val peerState = peerStates[teammate.userId]
                        TeammatePeerRow(
                            nickname = teammate.nickname,
                            peerState = peerState,
                            onToggleRemoteMute = {
                                // Toggle remote mute (local only - doesn't send signal)
                            }
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF1E293B))

                // My microphone toggle
                val muteColor = if (isMuted) Color(0xFFEF4444) else Color(0xFF22C55E)
                Button(
                    onClick = { voiceChannelManager.toggleMute() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = muteColor.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(muteColor.copy(alpha = 0.5f))
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isMuted) "🔇" else "🎤",
                            fontSize = 20.sp
                        )
                        Text(
                            text = if (isMuted) "음소거됨" else "마이크 켜짐",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = muteColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeammatePeerRow(
    nickname: String,
    peerState: PeerAudioState?,
    onToggleRemoteMute: () -> Unit
) {
    val statusColor = when (peerState?.connectionStatus) {
        PeerConnectionStatus.CONNECTED -> Color(0xFF22C55E)
        PeerConnectionStatus.CONNECTING -> Color(0xFFF59E0B)
        else -> Color(0xFF475569)
    }
    val statusText = when (peerState?.connectionStatus) {
        PeerConnectionStatus.CONNECTED -> "연결됨"
        PeerConnectionStatus.CONNECTING -> "연결 중"
        else -> "대기 중"
    }
    val isRemoteMuted = peerState?.isRemoteMuted == true

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Column {
                Text(
                    text = nickname,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE2E8F0)
                )
                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
        }

        // Remote mute button
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isRemoteMuted) Color(0xFF451A1A) else Color(0xFF1E3A2F))
                .clickable { onToggleRemoteMute() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isRemoteMuted) "🔇" else "🔊",
                fontSize = 14.sp
            )
        }
    }
}
