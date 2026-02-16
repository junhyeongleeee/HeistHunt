package com.heisthunt.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heisthunt.shared.models.RoomSettings
import kotlin.math.roundToInt

@Composable
fun GameSettingsDialog(
    onDismiss: () -> Unit,
    onConfirm: (RoomSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    var maxPlayers by remember { mutableStateOf(10) }
    var gameDurationMinutes by remember { mutableStateOf(30) }
    var escapeDurationMinutes by remember { mutableStateOf(5) }
    var policeRatio by remember { mutableStateOf(0.3f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "⚙️",
                    fontSize = 40.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "게임 설정",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Max Players Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "최대 인원",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "${maxPlayers}명",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = maxPlayers.toFloat(),
                        onValueChange = { maxPlayers = it.roundToInt() },
                        valueRange = 4f..20f,
                        steps = 15,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF3B82F6),
                            activeTrackColor = Color(0xFF3B82F6),
                            inactiveTrackColor = Color(0xFF334155)
                        )
                    )
                }

                // Game Duration Dropdown
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "전체 게임 시간",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "${gameDurationMinutes}분",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15, 30, 60).forEach { duration ->
                            FilterChip(
                                selected = gameDurationMinutes == duration,
                                onClick = { gameDurationMinutes = duration },
                                label = {
                                    Text(
                                        text = "${duration}분",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF3B82F6),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF334155),
                                    labelColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                }

                // Escape Duration Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "도망치는 시간",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "${escapeDurationMinutes}분",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = escapeDurationMinutes.toFloat(),
                        onValueChange = { escapeDurationMinutes = it.roundToInt() },
                        valueRange = 3f..10f,
                        steps = 6,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFDC2626),
                            activeTrackColor = Color(0xFFDC2626),
                            inactiveTrackColor = Color(0xFF334155)
                        )
                    )
                }

                // Police Ratio Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "경찰 비율",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "${(policeRatio * 100).roundToInt()}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = policeRatio,
                        onValueChange = { policeRatio = it },
                        valueRange = 0.2f..0.4f,
                        steps = 4,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF2563EB),
                            activeTrackColor = Color(0xFF2563EB),
                            inactiveTrackColor = Color(0xFF334155)
                        )
                    )
                }

                // Info text
                Text(
                    text = "도망치는 시간이 끝나면 경찰이 도둑을 추격할 수 있습니다.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val settings = RoomSettings(
                        maxPlayers = maxPlayers,
                        policeRatio = policeRatio,
                        gameDurationMinutes = gameDurationMinutes,
                        escapeDurationMinutes = escapeDurationMinutes,
                        password = null
                    )
                    onConfirm(settings)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B82F6)
                )
            ) {
                Text(
                    text = "방 만들기",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF64748B)
                )
            ) {
                Text(
                    text = "취소",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        containerColor = Color(0xFF0F172A),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}
