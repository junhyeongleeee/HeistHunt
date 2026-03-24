package com.heisthunt.app.ui.game

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import com.heisthunt.shared.models.Participant
import com.heisthunt.shared.models.PlayerRole

@Composable
fun PlayerListPanel(
    visible: Boolean,
    participants: List<Participant>,
    currentPhase: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                            text = "👥 전체 인원",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E8F0)
                        )
                        Text(
                            text = "${participants.size}명",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Text("✕", fontSize = 18.sp, color = Color(0xFF64748B))
                    }
                }

                HorizontalDivider(color = Color(0xFF1E293B))

                // Police section
                val police = participants.filter { it.role == PlayerRole.POLICE }
                if (police.isNotEmpty()) {
                    Text(
                        text = "경찰 ${police.size}명",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.sp
                    )
                    police.forEach { p ->
                        PlayerRow(participant = p, currentPhase = currentPhase)
                    }
                }

                // Thief section
                val thieves = participants.filter { it.role == PlayerRole.THIEF }
                if (thieves.isNotEmpty()) {
                    Text(
                        text = "도둑 ${thieves.size}명",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.sp
                    )
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(thieves) { p ->
                            PlayerRow(participant = p, currentPhase = currentPhase)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerRow(
    participant: Participant,
    currentPhase: String
) {
    val (statusText, statusColor) = when {
        participant.role == PlayerRole.POLICE && currentPhase == "ESCAPE" ->
            "대기중" to Color(0xFF3B82F6)
        participant.role == PlayerRole.POLICE ->
            "추격중" to Color(0xFFF59E0B)
        participant.isCaught ->
            "감옥" to Color(0xFFEF4444)
        else ->
            "도망중" to Color(0xFF22C55E)
    }

    val roleIcon = if (participant.role == PlayerRole.POLICE) "🚔" else "💰"

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
            Text(text = roleIcon, fontSize = 16.sp)
            Text(
                text = participant.nickname,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFE2E8F0)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Text(
                text = statusText,
                fontSize = 12.sp,
                color = statusColor
            )
        }
    }
}
