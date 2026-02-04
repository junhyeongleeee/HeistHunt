package com.heisthunt.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heisthunt.app.network.RoomWebSocketClient
import com.heisthunt.app.repository.RoomRepository
import com.heisthunt.shared.dto.RoomEvent
import com.heisthunt.shared.dto.RoomSummary
import com.heisthunt.shared.models.Participant
import com.heisthunt.shared.models.PlayerRole
import com.heisthunt.shared.models.Room
import com.heisthunt.shared.models.RoomSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RoomListUiState(
    val isLoading: Boolean = false,
    val rooms: List<RoomSummary> = emptyList(),
    val error: String? = null
)

data class RoomDetailUiState(
    val isLoading: Boolean = false,
    val room: Room? = null,
    val error: String? = null,
    val shouldNavigateToWaiting: Boolean = false,
    val shouldNavigateToGame: Boolean = false,
    val gameId: String? = null,
    val myRole: PlayerRole? = null,
    val gameStartTime: kotlinx.datetime.Instant? = null,
    val escapeDurationSeconds: Long = 300L,
    val totalDurationSeconds: Long = 1200L
)

class RoomViewModel(
    private val roomRepository: RoomRepository,
    private val baseUrl: String = "10.0.2.2:8080",
    private val getAccessToken: () -> String? = { roomRepository.getAccessToken() }
) : ViewModel() {

    private val _listState = MutableStateFlow(RoomListUiState())
    val listState: StateFlow<RoomListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(RoomDetailUiState())
    val detailState: StateFlow<RoomDetailUiState> = _detailState.asStateFlow()

    private val wsClient = RoomWebSocketClient(baseUrl, getAccessToken)
    val wsConnectionState = wsClient.connectionState

    init {
        // Listen to WebSocket events
        viewModelScope.launch {
            wsClient.events.collect { event ->
                handleWebSocketEvent(event)
            }
        }
    }

    private fun handleWebSocketEvent(event: RoomEvent) {
        println("📥 [RoomViewModel] Handling WebSocket event: ${event::class.simpleName}")

        // Handle GameStarted event first (before room check)
        if (event is RoomEvent.GameStarted) {
            println("🎮 [RoomViewModel] *** GAME STARTED *** with ID: ${event.gameId}")
            println("🎮 [RoomViewModel] startTime: ${event.startTime}")
            println("🎮 [RoomViewModel] escapeDurationSeconds: ${event.escapeDurationSeconds}")
            println("🎮 [RoomViewModel] totalDurationSeconds: ${event.totalDurationSeconds}")
            viewModelScope.launch {
                val roomId = _detailState.value.room?.id
                println("🎮 [RoomViewModel] Current roomId: $roomId")
                if (roomId != null) {
                    println("📡 [RoomViewModel] Reloading room to get assigned roles...")
                    // Reload room to get updated participant roles
                    roomRepository.getRoom(roomId)
                        .onSuccess { updatedRoom ->
                            println("✅ [RoomViewModel] Room reloaded successfully")
                            println("✅ [RoomViewModel] Participants count: ${updatedRoom.participants.size}")
                            // Find current user's role from participants
                            val currentUserId = com.heisthunt.app.di.AppModule.tokenStorage.userId
                            val myParticipant = updatedRoom.participants.find { it.userId == currentUserId }
                            val myRole = myParticipant?.role

                            println("🎭 [RoomViewModel] Current userId: $currentUserId")
                            println("🎭 [RoomViewModel] My assigned role: $myRole")

                            _detailState.value = _detailState.value.copy(
                                room = updatedRoom,
                                shouldNavigateToGame = true,
                                gameId = event.gameId,
                                myRole = myRole,
                                gameStartTime = event.startTime,
                                escapeDurationSeconds = event.escapeDurationSeconds,
                                totalDurationSeconds = event.totalDurationSeconds
                            )
                            println("🚀 [RoomViewModel] Navigation state set!")
                            println("🚀 [RoomViewModel] shouldNavigateToGame: true")
                            println("🚀 [RoomViewModel] gameId: ${event.gameId}")
                            println("🚀 [RoomViewModel] myRole: $myRole")
                        }
                        .onFailure { exception ->
                            println("❌ [RoomViewModel] Failed to reload room: ${exception.message}")
                        }
                } else {
                    println("❌ [RoomViewModel] roomId is null, cannot reload room!")
                }
            }
            return
        }

        val currentRoom = _detailState.value.room ?: return

        when (event) {
            is RoomEvent.ParticipantJoined -> {
                // Add new participant if not already in the list
                val updatedParticipants = currentRoom.participants
                    .filter { it.userId != event.participant.userId }
                    .plus(event.participant)

                val updatedRoom = currentRoom.copy(participants = updatedParticipants)
                _detailState.value = _detailState.value.copy(room = updatedRoom)
            }

            is RoomEvent.ParticipantLeft -> {
                // Remove participant from the list
                val updatedParticipants = currentRoom.participants
                    .filter { it.userId != event.userId }

                val updatedRoom = currentRoom.copy(participants = updatedParticipants)
                _detailState.value = _detailState.value.copy(room = updatedRoom)
            }

            is RoomEvent.ParticipantReady -> {
                // Update participant's ready status
                val updatedParticipants = currentRoom.participants.map { participant ->
                    if (participant.userId == event.userId) {
                        participant.copy(isReady = event.isReady)
                    } else {
                        participant
                    }
                }

                val updatedRoom = currentRoom.copy(participants = updatedParticipants)
                _detailState.value = _detailState.value.copy(room = updatedRoom)
            }

            is RoomEvent.RoleSelected -> {
                println("📡 RoleSelected event received: userId=${event.userId}, role=${event.role}")
                // Update participant's selected role
                val updatedParticipants = currentRoom.participants.map { participant ->
                    if (participant.userId == event.userId) {
                        println("✅ Updating participant ${participant.nickname} role to ${event.role}")
                        participant.copy(selectedRole = event.role)
                    } else {
                        participant
                    }
                }

                val updatedRoom = currentRoom.copy(participants = updatedParticipants)
                _detailState.value = _detailState.value.copy(room = updatedRoom)
                println("🔄 Room state updated with new roles")
            }

            is RoomEvent.RoomUpdated -> {
                // Replace entire room state
                _detailState.value = _detailState.value.copy(room = event.room)
            }

            is RoomEvent.GameStarted -> {
                // Already handled above (before room check)
                // This branch will never be reached
            }
        }
    }

    fun loadRooms() {
        viewModelScope.launch {
            _listState.value = _listState.value.copy(isLoading = true, error = null)

            roomRepository.getRooms()
                .onSuccess { response ->
                    _listState.value = RoomListUiState(
                        isLoading = false,
                        rooms = response.rooms
                    )
                }
                .onFailure { exception ->
                    _listState.value = _listState.value.copy(
                        isLoading = false,
                        error = exception.message
                    )
                }
        }
    }

    fun createRoom(name: String, settings: RoomSettings = RoomSettings()) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isLoading = true, error = null)

            roomRepository.createRoom(name, settings)
                .onSuccess { room ->
                    _detailState.value = RoomDetailUiState(
                        isLoading = false,
                        room = room,
                        shouldNavigateToWaiting = true
                    )
                }
                .onFailure { exception ->
                    _detailState.value = _detailState.value.copy(
                        isLoading = false,
                        error = exception.message
                    )
                }
        }
    }

    fun resetNavigationState() {
        _detailState.value = _detailState.value.copy(shouldNavigateToWaiting = false)
    }

    fun resetGameNavigationState() {
        _detailState.value = _detailState.value.copy(shouldNavigateToGame = false)
    }

    fun joinRoom(code: String, password: String? = null) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isLoading = true, error = null)

            roomRepository.joinRoom(code, password)
                .onSuccess { room ->
                    _detailState.value = RoomDetailUiState(
                        isLoading = false,
                        room = room,
                        shouldNavigateToWaiting = true
                    )
                }
                .onFailure { exception ->
                    _detailState.value = _detailState.value.copy(
                        isLoading = false,
                        error = exception.message
                    )
                }
        }
    }

    fun joinRoomByCode(code: String) {
        joinRoom(code, password = null)
    }

    fun loadRoom(roomId: String) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isLoading = true, error = null)

            roomRepository.getRoom(roomId)
                .onSuccess { room ->
                    _detailState.value = RoomDetailUiState(
                        isLoading = false,
                        room = room
                    )
                }
                .onFailure { exception ->
                    _detailState.value = _detailState.value.copy(
                        isLoading = false,
                        error = exception.message
                    )
                }
        }
    }

    fun toggleReady(roomId: String) {
        viewModelScope.launch {
            roomRepository.toggleReady(roomId)
                .onSuccess { room ->
                    _detailState.value = _detailState.value.copy(room = room)
                }
                .onFailure { exception ->
                    _detailState.value = _detailState.value.copy(error = exception.message)
                }
        }
    }

    fun selectRole(roomId: String, role: PlayerRole) {
        println("🎯 RoomViewModel.selectRole called: roomId=$roomId, role=$role")
        viewModelScope.launch {
            println("🎯 Calling roomRepository.selectRole...")
            roomRepository.selectRole(roomId, role)
                .onSuccess { room ->
                    println("✅ Role selection success! Updated room: ${room.participants.find { it.selectedRole != null }?.selectedRole}")
                    _detailState.value = _detailState.value.copy(room = room)
                }
                .onFailure { exception ->
                    println("❌ Role selection failed: ${exception.message}")
                    _detailState.value = _detailState.value.copy(error = exception.message)
                }
        }
    }

    fun leaveRoom(roomId: String) {
        viewModelScope.launch {
            roomRepository.leaveRoom(roomId)
                .onSuccess {
                    _detailState.value = RoomDetailUiState()
                }
                .onFailure { exception ->
                    _detailState.value = _detailState.value.copy(error = exception.message)
                }
        }
    }

    fun clearError() {
        _listState.value = _listState.value.copy(error = null)
        _detailState.value = _detailState.value.copy(error = null)
    }

    fun restoreGameState(
        gameId: String,
        myRole: PlayerRole,
        gameStartTime: kotlinx.datetime.Instant,
        escapeDurationSeconds: Long,
        totalDurationSeconds: Long
    ) {
        _detailState.value = _detailState.value.copy(
            gameId = gameId,
            myRole = myRole,
            gameStartTime = gameStartTime,
            escapeDurationSeconds = escapeDurationSeconds,
            totalDurationSeconds = totalDurationSeconds,
            shouldNavigateToGame = true
        )
    }

    fun clearRoom() {
        _detailState.value = RoomDetailUiState()
    }

    fun clearNavigationFlag() {
        _detailState.value = _detailState.value.copy(
            shouldNavigateToGame = false,
            shouldNavigateToWaiting = false
        )
    }

    fun startGame(roomId: String) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isLoading = true, error = null)

            roomRepository.startGame(roomId)
                .onSuccess { response ->
                    _detailState.value = _detailState.value.copy(
                        isLoading = false,
                        shouldNavigateToGame = true,
                        gameId = response.gameId,
                        myRole = response.yourRole
                    )
                }
                .onFailure { exception ->
                    _detailState.value = _detailState.value.copy(
                        isLoading = false,
                        error = exception.message
                    )
                }
        }
    }

    fun connectToRoom(roomId: String) {
        viewModelScope.launch {
            println("Connecting to WebSocket for room: $roomId")
            wsClient.connect(roomId)
        }
    }

    fun disconnectFromRoom() {
        viewModelScope.launch {
            println("Disconnecting from WebSocket")
            wsClient.disconnect()
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            wsClient.disconnect()
        }
    }
}
