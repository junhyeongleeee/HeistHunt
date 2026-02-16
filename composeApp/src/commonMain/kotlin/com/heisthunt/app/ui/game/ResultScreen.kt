package com.heisthunt.app.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    onBackToLobby: () -> Unit
) {
    println("🏁 [ResultScreen] Rendering result screen")
    println("   Winner: ${result.winner}")
    println("   Duration: ${result.duration}s")
    println("   Players: ${result.players.size}")

    val isPoliceWin = result.winner == GameWinner.POLICE
    val winnerColor = if (isPoliceWin) Color(0xFF2196F3) else Color(0xFFFF9800)
    val winnerIcon = if (isPoliceWin) "🚔" else "💰"
    val winnerText = if (isPoliceWin) "경찰 승리!" else "도둑 승리!"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Winner Title
        Text(
            text = winnerIcon,
            fontSize = 72.sp,
            textAlign = TextAlign.Center
        )

        Text(
            text = winnerText,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = winnerColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Game Statistics Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "게임 통계",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                // Play time
                StatRow(
                    label = "플레이 시간",
                    value = formatDuration(result.duration)
                )

                // Caught thieves
                val caughtCount = result.players.count { it.role == PlayerRole.THIEF && it.isCaught }
                val totalThieves = result.players.count { it.role == PlayerRole.THIEF }
                StatRow(
                    label = "잡힌 도둑",
                    value = "$caughtCount / $totalThieves"
                )
            }
        }

        // Players List Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "플레이어",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                result.players.forEach { player ->
                    PlayerRow(
                        nickname = player.nickname,
                        role = player.role,
                        isCaught = player.isCaught
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Back to Lobby Button
        Button(
            onClick = {
                println("🔙 [ResultScreen] Back to lobby button clicked")
                onBackToLobby()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "로비로",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PlayerRow(
    nickname: String,
    role: PlayerRole,
    isCaught: Boolean
) {
    val roleColor = when (role) {
        PlayerRole.POLICE -> Color(0xFF2196F3)
        PlayerRole.THIEF -> Color(0xFFFF9800)
    }
    val roleText = when (role) {
        PlayerRole.POLICE -> "경찰"
        PlayerRole.THIEF -> "도둑"
    }
    val statusText = if (role == PlayerRole.THIEF && isCaught) " (체포됨)" else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = roleColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = nickname,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$roleText$statusText",
            fontSize = 14.sp,
            color = roleColor,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}"
}
