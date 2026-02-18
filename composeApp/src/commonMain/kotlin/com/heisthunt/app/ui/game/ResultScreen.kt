package com.heisthunt.app.ui.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heisthunt.shared.dto.GameResultResponse
import com.heisthunt.shared.models.GameWinner
import com.heisthunt.shared.models.PlayerRole

@Composable
fun ResultScreen(
    result: GameResultResponse,
    myUserId: String = "",
    onBackToLobby: () -> Unit
) {
    println("🏁 [ResultScreen] Rendering result screen")
    println("   Winner: ${result.winner}")
    println("   Duration: ${result.duration}s")
    println("   Players: ${result.players.size}")
    println("   myUserId: $myUserId")

    // 개인 승패 판정
    val myPlayer = result.players.find { it.userId == myUserId }
    val myRole = myPlayer?.role
    val isPoliceWin = result.winner == GameWinner.POLICE
    val isMyTeamWon = when (myRole) {
        PlayerRole.POLICE -> isPoliceWin
        PlayerRole.THIEF -> !isPoliceWin
        null -> isPoliceWin // fallback: 알 수 없으면 팀 결과 표시
    }
    // 체포된 도둑은 팀이 이겨도 개인 패배
    val isPersonalWin = isMyTeamWon && !(myRole == PlayerRole.THIEF && myPlayer?.isCaught == true)

    println("   myRole: $myRole, isMyTeamWon: $isMyTeamWon, isPersonalWin: $isPersonalWin")

    // 등장 애니메이션
    var animTrigger by remember { mutableStateOf(false) }
    val animScale by animateFloatAsState(
        targetValue = if (animTrigger) 1f else 0.8f,
        animationSpec = tween(durationMillis = 500),
        label = "resultScale"
    )
    val animAlpha by animateFloatAsState(
        targetValue = if (animTrigger) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "resultAlpha"
    )
    LaunchedEffect(Unit) { animTrigger = true }

    // 색상
    val personalColor = if (isPersonalWin) Color(0xFF22C55E) else Color(0xFFEF4444)
    val teamColor = if (isPoliceWin) Color(0xFF3B82F6) else Color(0xFFEF4444)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020617))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 64.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 개인 결과 (애니메이션)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer(
                scaleX = animScale,
                scaleY = animScale,
                alpha = animAlpha
            )
        ) {
            Text(
                text = if (isPersonalWin) "🎉" else "💀",
                fontSize = 80.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isPersonalWin) "승리!" else "패배!",
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                color = personalColor,
                textAlign = TextAlign.Center
            )
        }

        // 팀 결과
        Text(
            text = if (isPoliceWin) "경찰팀 승리" else "도둑팀 승리",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = teamColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(animAlpha)
        )

        // 게임 통계 카드
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "게임 통계",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = Color(0xFF64748B)
                )
                HorizontalDivider(color = Color(0xFF1E293B))

                val caughtCount = result.players.count { it.role == PlayerRole.THIEF && it.isCaught }
                val totalThieves = result.players.count { it.role == PlayerRole.THIEF }

                StatRow(label = "플레이 시간", value = formatDuration(result.duration))
                StatRow(label = "체포된 도둑", value = "$caughtCount / $totalThieves")
            }
        }

        // 플레이어 목록 카드
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "플레이어",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = Color(0xFF64748B)
                )
                HorizontalDivider(color = Color(0xFF1E293B))

                // 경찰 먼저, 도둑 다음
                val sorted = result.players.sortedWith(
                    compareBy({ it.role != PlayerRole.POLICE }, { it.nickname })
                )
                sorted.forEach { player ->
                    PlayerRow(
                        player = player,
                        isMe = player.userId == myUserId,
                        teamWon = (player.role == PlayerRole.POLICE && isPoliceWin)
                               || (player.role == PlayerRole.THIEF && !isPoliceWin)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 로비로 버튼
        Button(
            onClick = {
                println("🔙 [ResultScreen] Back to lobby button clicked")
                onBackToLobby()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E293B)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "로비로 돌아가기",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE2E8F0)
            )
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF94A3B8)
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE2E8F0)
        )
    }
}

@Composable
private fun PlayerRow(
    player: com.heisthunt.shared.dto.PlayerResult,
    isMe: Boolean,
    teamWon: Boolean
) {
    val roleIcon = if (player.role == PlayerRole.POLICE) "🚔" else "💰"
    val roleColor = if (player.role == PlayerRole.POLICE) Color(0xFF3B82F6) else Color(0xFFEF4444)
    val borderColor = if (isMe) Color(0xFF64748B) else Color.Transparent

    val (statusIcon, statusText, statusColor) = when {
        player.role == PlayerRole.THIEF && player.isCaught ->
            Triple("⛓", "체포됨", Color(0xFFEF4444))
        player.role == PlayerRole.THIEF && !player.isCaught ->
            Triple("✅", "탈출", Color(0xFF22C55E))
        teamWon ->
            Triple("🏅", "승리", Color(0xFF22C55E))
        else ->
            Triple("", "패배", Color(0xFF64748B))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = roleColor.copy(alpha = 0.07f),
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = if (isMe) 1.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = roleIcon, fontSize = 18.sp)
            Column {
                Text(
                    text = player.nickname + if (isMe) " (나)" else "",
                    fontSize = 15.sp,
                    fontWeight = if (isMe) FontWeight.Bold else FontWeight.Medium,
                    color = Color(0xFFE2E8F0)
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (statusIcon.isNotEmpty()) {
                Text(text = statusIcon, fontSize = 14.sp)
            }
            Text(
                text = statusText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}"
}
