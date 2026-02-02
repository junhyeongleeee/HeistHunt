package com.heisthunt.app.ui.room

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.heisthunt.app.viewmodel.RoomDetailUiState
import com.heisthunt.shared.models.RoomSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoomScreen(
    uiState: RoomDetailUiState,
    onCreateRoom: (String, RoomSettings) -> Unit,
    onBack: () -> Unit,
    onRoomCreated: () -> Unit,
    modifier: Modifier = Modifier
) {
    var roomName by remember { mutableStateOf("") }
    var maxPlayers by remember { mutableStateOf("10") }
    var policeRatio by remember { mutableStateOf("30") }
    var gameDuration by remember { mutableStateOf("30") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(uiState.room) {
        if (uiState.room != null) {
            onRoomCreated()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("새 게임 방") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = roomName,
                onValueChange = { roomName = it },
                label = { Text("방 이름") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = maxPlayers,
                onValueChange = { maxPlayers = it.filter { c -> c.isDigit() }.take(2) },
                label = { Text("최대 인원 (2-30)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = policeRatio,
                onValueChange = { policeRatio = it.filter { c -> c.isDigit() }.take(2) },
                label = { Text("경찰 비율 (%)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = gameDuration,
                onValueChange = { gameDuration = it.filter { c -> c.isDigit() }.take(3) },
                label = { Text("게임 시간 (분)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("비밀번호 (선택)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.error != null) {
                Text(
                    text = uiState.error,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val settings = RoomSettings(
                        maxPlayers = maxPlayers.toIntOrNull() ?: 10,
                        policeRatio = (policeRatio.toIntOrNull() ?: 30) / 100f,
                        gameDurationMinutes = gameDuration.toIntOrNull() ?: 30,
                        password = password.ifBlank { null }
                    )
                    onCreateRoom(roomName, settings)
                },
                enabled = !uiState.isLoading && roomName.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("방 만들기")
                }
            }
        }
    }
}
