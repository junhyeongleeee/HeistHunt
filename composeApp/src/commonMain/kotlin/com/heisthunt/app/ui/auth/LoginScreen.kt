package com.heisthunt.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heisthunt.app.viewmodel.AuthUiState

@Composable
fun LoginScreen(
    uiState: AuthUiState,
    onLogin: (String, String) -> Unit,
    onGoogleLogin: () -> Unit = {},
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020617)) // slate-950
    ) {
        // Background decorations - blue circle (top-left)
        Box(
            modifier = Modifier
                .offset(x = (-150).dp, y = (-150).dp)
                .size(400.dp)
                .background(
                    color = Color(0xFF1E3A8A).copy(alpha = 0.15f), // blue-900/20
                    shape = CircleShape
                )
        )

        // Background decorations - red circle (bottom-right)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 150.dp, y = 150.dp)
                .size(400.dp)
                .background(
                    color = Color(0xFF991B1B).copy(alpha = 0.15f), // red-900/20
                    shape = CircleShape
                )
        )

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // App name section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 64.dp)
            ) {
                // HEIST (Red)
                Text(
                    text = "HEIST",
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2).sp,
                    color = Color(0xFFDC2626), // red-600
                    lineHeight = 72.sp
                )

                // HUNT (Blue)
                Text(
                    text = "HUNT",
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2).sp,
                    color = Color(0xFF3B82F6), // blue-500
                    modifier = Modifier.offset(y = (-10).dp),
                    lineHeight = 72.sp
                )

                // Subtitle with decorative lines
                Row(
                    modifier = Modifier.padding(top = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(1.dp)
                            .background(Color(0xFF334155)) // slate-700
                    )

                    Text(
                        text = "THE URBAN CHASE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = Color(0xFF94A3B8), // slate-400
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(1.dp)
                            .background(Color(0xFF334155)) // slate-700
                    )
                }
            }

            // Social login buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Google button
                Button(
                    onClick = onGoogleLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .semantics {
                            contentDescription = "Google로 계속하기"
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF0F172A) // slate-900
                    )
                ) {
                    Text(
                        text = "Google로 계속하기",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                // Kakao button
                Button(
                    onClick = { /* TODO: Kakao login */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFEE500),
                        contentColor = Color(0xFF0F172A) // slate-900
                    )
                ) {
                    Text(
                        text = "카카오로 계속하기",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                // Apple button
                Button(
                    onClick = { /* TODO: Apple login */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E293B), // slate-800
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color(0xFF334155) // slate-700
                    )
                ) {
                    Text(
                        text = "Apple로 계속하기",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer
            Text(
                text = "계속 진행함으로써 HeistHunt의 서비스 약관에 동의하게 됩니다.",
                fontSize = 10.sp,
                color = Color(0xFF64748B).copy(alpha = 0.6f), // slate-500 with opacity
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}
