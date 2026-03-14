package com.heisthunt.app.ui.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    isAuthReady: Boolean,       // 자동로그인 체크 완료 여부
    onNavigate: () -> Unit      // 화면 전환 콜백
) {
    // Truveri 패턴: 최소 표시 시간(1.5s) + 자동로그인 완료 둘 다 충족 시 전환
    var minTimeElapsed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1500)
        minTimeElapsed = true
    }

    LaunchedEffect(minTimeElapsed, isAuthReady) {
        if (minTimeElapsed && isAuthReady) {
            onNavigate()
        }
    }

    // 로고 애니메이션
    val alpha by rememberInfiniteTransition(label = "logo").animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020617)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row {
                Text(
                    text = "HEIST",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFDC2626).copy(alpha = alpha),
                    letterSpacing = 2.sp
                )
                Text(
                    text = "HUNT",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF3B82F6).copy(alpha = alpha),
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "경찰과 도둑",
                fontSize = 14.sp,
                color = Color(0xFF64748B),
                letterSpacing = 4.sp
            )
        }
    }
}
