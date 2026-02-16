package com.heisthunt.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heisthunt.app.location.LocationService
import com.heisthunt.app.network.GameWebSocketClient
import com.heisthunt.app.repository.GameRepository
import com.heisthunt.shared.dto.GameStateResponse
import com.heisthunt.shared.dto.WebSocketMessage
import com.heisthunt.shared.models.GameWinner
import com.heisthunt.shared.models.Location
import com.heisthunt.shared.models.PlayerLocation
import com.heisthunt.shared.models.PlayerRole
import com.heisthunt.shared.utils.GeoUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CatchRequestState(
    val policeUserId: String,
    val policeNickname: String,
    val thiefUserId: String
)

data class PoliceViolationState(
    val policeUserId: String,
    val distanceFromJail: Double,
    val timestamp: kotlinx.datetime.Instant
)

data class ProximityAlertState(
    val enemyRole: PlayerRole,
    val count: Int
)

data class GameUiState(
    val gameId: String,
    val myRole: PlayerRole,
    val gameStatus: GameStateResponse? = null,
    val playerLocations: List<PlayerLocation> = emptyList(),
    val disconnectedPlayerIds: Set<String> = emptySet(),
    val myLocation: Location? = null,
    val isLocationTracking: Boolean = false,
    val error: String? = null,
    val connectionState: GameWebSocketClient.ConnectionState = GameWebSocketClient.ConnectionState.DISCONNECTED,
    val isGameEnded: Boolean = false,
    val winner: GameWinner? = null,
    // Catch mechanism
    val nearbyThieves: List<PlayerLocation> = emptyList(), // For police: thieves within 5m
    val catchRequest: CatchRequestState? = null, // For thief: incoming catch request
    val jailLocation: Location? = null, // For caught thief: where to return
    val isCaught: Boolean = false, // If current player is caught
    // Local timer (hybrid approach)
    val startTime: kotlinx.datetime.Instant? = null,
    val escapeDurationSeconds: Long = 300L,
    val totalDurationSeconds: Long = 1200L,
    val currentPhase: String = "ESCAPE",
    val localRemainingSeconds: Long = 300L,
    val localEscapeRemainingSeconds: Long = 300L,
    // Phase-based alerts
    val policeViolation: PoliceViolationState? = null, // For thief in ESCAPE: police left jail
    val nearbyEnemies: ProximityAlertState? = null // For both in CHASE: enemies within 5m
)

class GameViewModel(
    private val gameRepository: GameRepository,
    private val gameId: String,
    private val myRole: PlayerRole,
    private val locationService: LocationService,
    private val wsClient: GameWebSocketClient,
    private val startTime: kotlinx.datetime.Instant?,
    private val escapeDurationSeconds: Long,
    private val totalDurationSeconds: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        GameUiState(
            gameId = gameId,
            myRole = myRole,
            startTime = startTime,
            escapeDurationSeconds = escapeDurationSeconds,
            totalDurationSeconds = totalDurationSeconds
        )
    )
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        println("🎮 GameViewModel initialized: gameId=$gameId, myRole=$myRole")

        // Connect to WebSocket
        viewModelScope.launch {
            wsClient.connect(gameId)
        }

        // Observe WebSocket connection state
        viewModelScope.launch {
            wsClient.connectionState.collect { state ->
                println("🔌 WebSocket state changed: $state")
                _uiState.update { it.copy(connectionState = state) }
            }
        }

        // Observe WebSocket events
        viewModelScope.launch {
            wsClient.events.collect { event ->
                handleWebSocketEvent(event)
            }
        }

        // Start local timer for smooth UI updates
        if (startTime != null) {
            viewModelScope.launch {
                while (!_uiState.value.isGameEnded) {
                    val now = kotlinx.datetime.Clock.System.now()
                    val elapsed = (now - startTime).inWholeSeconds

                    // Calculate remaining time for total game
                    val totalRemaining = (totalDurationSeconds - elapsed).coerceAtLeast(0)

                    // Calculate remaining time for escape phase
                    val escapeRemaining = (escapeDurationSeconds - elapsed).coerceAtLeast(0)

                    // Determine current phase
                    val phase = if (elapsed < escapeDurationSeconds) "ESCAPE" else "CHASE"

                    _uiState.update {
                        it.copy(
                            currentPhase = phase,
                            localRemainingSeconds = totalRemaining,
                            localEscapeRemainingSeconds = escapeRemaining
                        )
                    }

                    // Update every second for smooth UI
                    kotlinx.coroutines.delay(1000L)
                }
            }
        }
    }

    private fun handleWebSocketEvent(event: WebSocketMessage) {
        when (event) {
            is WebSocketMessage.PlayerLocations -> {
                println("Received ${event.locations.size} player locations")
                val now = kotlinx.datetime.Clock.System.now()

                // Identify disconnected players (no update for 15 seconds)
                val disconnected = event.locations
                    .filter { (now - it.lastUpdateTimestamp).inWholeSeconds > 15 }
                    .map { it.userId }
                    .toSet()

                _uiState.update {
                    it.copy(
                        playerLocations = event.locations,
                        disconnectedPlayerIds = disconnected
                    )
                }
                // Calculate nearby enemies
                updateNearbyThieves() // For police
                updateNearbyPolice()  // For thieves
            }
            is WebSocketMessage.PlayerCaught -> {
                println("Player caught: ${event.nickname}")
                _uiState.update {
                    it.copy(error = "${event.nickname} has been caught!")
                }
                // Reload game status to get updated counts
                loadGameStatus()
            }
            is WebSocketMessage.CatchRequest -> {
                // Thief receives catch request from police
                println("Catch request from police ${event.policeNickname}")
                _uiState.update {
                    it.copy(
                        catchRequest = CatchRequestState(
                            policeUserId = event.policeUserId,
                            policeNickname = event.policeNickname,
                            thiefUserId = event.thiefUserId
                        )
                    )
                }
            }
            is WebSocketMessage.CatchConfirmed -> {
                // All players receive confirmation that a thief was caught
                println("Thief ${event.thiefNickname} confirmed caught, returning to jail")
                _uiState.update {
                    it.copy(error = "${event.thiefNickname}가 체포되었습니다!")
                }

                // If I'm the caught thief, update my state
                val currentState = _uiState.value
                if (currentState.myRole == PlayerRole.THIEF) {
                    // Check if it's me by comparing with my location or userId
                    // For now, we'll set jail location for navigation
                    _uiState.update {
                        it.copy(
                            jailLocation = Location(
                                latitude = event.jailLatitude,
                                longitude = event.jailLongitude,
                                accuracy = null,
                                timestamp = kotlinx.datetime.Clock.System.now()
                            )
                        )
                    }
                }

                loadGameStatus()
            }
            is WebSocketMessage.CatchRejected -> {
                // All players receive notification that thief rejected
                println("Thief ${event.thiefUserId} rejected catch: ${event.reason}")
                _uiState.update {
                    it.copy(error = event.reason)
                }
            }
            is WebSocketMessage.PhaseChanged -> {
                println("Phase changed to ${event.phase}: ${event.message}")
                _uiState.update {
                    it.copy(error = event.message)
                }
                // 게임 상태 새로고침하여 phase 업데이트
                loadGameStatus()
            }
            is WebSocketMessage.GameEnded -> {
                println("Game ended, winner: ${event.winner}")
                _uiState.update {
                    it.copy(
                        isGameEnded = true,
                        winner = event.winner,
                        error = "Game Over! ${event.winner.name} wins!"
                    )
                }
                stopLocationTracking()
            }
            is WebSocketMessage.Error -> {
                println("WebSocket error: ${event.message}")
                _uiState.update { it.copy(error = event.message) }
            }
            is WebSocketMessage.PoliceViolation -> {
                println("Police violation detected: ${event.policeUserId} is ${event.distanceFromJail}m from jail")
                _uiState.update {
                    it.copy(
                        policeViolation = PoliceViolationState(
                            policeUserId = event.policeUserId,
                            distanceFromJail = event.distanceFromJail,
                            timestamp = kotlinx.datetime.Clock.System.now()
                        )
                    )
                }
                // 5초 후 자동 제거
                viewModelScope.launch {
                    kotlinx.coroutines.delay(5000L)
                    _uiState.update { it.copy(policeViolation = null) }
                }
            }
            else -> {
                println("Unhandled WebSocket event: ${event::class.simpleName}")
            }
        }
    }

    private fun updateNearbyThieves() {
        val currentState = _uiState.value

        // Only police need to detect nearby thieves
        if (currentState.myRole != PlayerRole.POLICE) return

        // Only in CHASE phase
        if (currentState.currentPhase != "CHASE") return

        val myLocation = currentState.myLocation ?: return
        val playerLocations = currentState.playerLocations

        // Find thieves within 5 meters
        val nearby = playerLocations.filter { player ->
            player.role == PlayerRole.THIEF &&
            GeoUtils.isWithinRadius(
                lat1 = myLocation.latitude,
                lon1 = myLocation.longitude,
                lat2 = player.location.latitude,
                lon2 = player.location.longitude,
                radiusMeters = 5.0
            )
        }

        val previousCount = currentState.nearbyThieves.size
        if (nearby.size != previousCount) {
            println("Nearby thieves: ${nearby.size}")
            _uiState.update {
                it.copy(
                    nearbyThieves = nearby,
                    nearbyEnemies = if (nearby.isNotEmpty()) {
                        ProximityAlertState(
                            enemyRole = PlayerRole.THIEF,
                            count = nearby.size
                        )
                    } else null
                )
            }
        }
    }

    private fun updateNearbyPolice() {
        val currentState = _uiState.value

        // Only thieves need to detect nearby police
        if (currentState.myRole != PlayerRole.THIEF) return

        // Only in CHASE phase
        if (currentState.currentPhase != "CHASE") return

        val myLocation = currentState.myLocation ?: return
        val playerLocations = currentState.playerLocations

        // Find police within 5 meters
        val nearby = playerLocations.filter { player ->
            player.role == PlayerRole.POLICE &&
            GeoUtils.isWithinRadius(
                lat1 = myLocation.latitude,
                lon1 = myLocation.longitude,
                lat2 = player.location.latitude,
                lon2 = player.location.longitude,
                radiusMeters = 5.0
            )
        }

        val previousCount = currentState.nearbyEnemies?.count ?: 0
        if (nearby.size != previousCount) {
            println("Nearby police: ${nearby.size}")
            _uiState.update {
                it.copy(
                    nearbyEnemies = if (nearby.isNotEmpty()) {
                        ProximityAlertState(
                            enemyRole = PlayerRole.POLICE,
                            count = nearby.size
                        )
                    } else null
                )
            }
        }
    }

    fun startLocationTracking() {
        if (_uiState.value.isLocationTracking) {
            println("Location tracking already started")
            return
        }

        if (!locationService.isLocationEnabled()) {
            _uiState.update {
                it.copy(error = "Location services are disabled. Please enable GPS.")
            }
            return
        }

        locationService.startLocationUpdates(
            intervalMillis = 5000L,
            onLocationUpdate = { location ->
                println("Location updated: lat=${location.latitude}, lon=${location.longitude}")
                _uiState.update { it.copy(myLocation = location, error = null) }

                // Send location to server via WebSocket
                viewModelScope.launch {
                    wsClient.sendLocation(location)
                }

                // Check for nearby enemies
                updateNearbyThieves() // For police
                updateNearbyPolice()  // For thieves
            },
            onError = { error ->
                println("Location error: $error")
                _uiState.update { it.copy(error = error) }
            }
        )

        _uiState.update { it.copy(isLocationTracking = true) }
        println("Location tracking started")
    }

    fun stopLocationTracking() {
        locationService.stopLocationUpdates()
        _uiState.update { it.copy(isLocationTracking = false) }
        println("Location tracking stopped")
    }

    fun loadGameStatus() {
        viewModelScope.launch {
            println("📡 Requesting game status for gameId=$gameId")
            gameRepository.getGameStatus(gameId)
                .onSuccess { status ->
                    if (status != null) {
                        println("✅ Game status received:")
                        println("   - phase: ${status.phase}")
                        println("   - remainingTimeSeconds: ${status.remainingTimeSeconds}")
                        println("   - escapeTimeRemaining: ${status.escapeTimeRemaining}")
                        println("   - remainingThieves: ${status.remainingThieves}/${status.totalThieves}")

                        _uiState.update { it.copy(gameStatus = status) }

                        // UI state 업데이트 확인
                        val currentState = _uiState.value
                        println("📱 UI state updated:")
                        println("   - gameStatus.phase: ${currentState.gameStatus?.phase}")
                        println("   - gameStatus.escapeTimeRemaining: ${currentState.gameStatus?.escapeTimeRemaining}")
                    } else {
                        println("⚠️ Game status is null")
                    }
                }
                .onFailure { error ->
                    // 타이머 업데이트를 위한 주기적 호출에서는 에러를 조용히 처리
                    println("❌ Error loading game status: ${error.message}")
                    error.printStackTrace()
                    // UI에는 표시하지 않음 (3초마다 호출되므로 에러 메시지가 계속 뜨는 것 방지)
                }
        }
    }

    // Police: Request to catch a thief
    fun requestCatch(thiefUserId: String, policeUserId: String, policeNickname: String) {
        viewModelScope.launch {
            wsClient.sendCatchRequest(
                policeUserId = policeUserId,
                policeNickname = policeNickname,
                thiefUserId = thiefUserId
            )
            println("Police $policeUserId requested to catch thief $thiefUserId")
        }
    }

    // Thief: Confirm being caught
    fun confirmCatch(thiefUserId: String, thiefNickname: String) {
        viewModelScope.launch {
            // Get jail location (first location where game started)
            val jailLoc = _uiState.value.myLocation
            if (jailLoc != null) {
                wsClient.sendCatchConfirmed(
                    thiefUserId = thiefUserId,
                    thiefNickname = thiefNickname,
                    jailLatitude = jailLoc.latitude,
                    jailLongitude = jailLoc.longitude
                )
                _uiState.update {
                    it.copy(
                        isCaught = true,
                        catchRequest = null,
                        error = "체포당했습니다! 감옥으로 돌아가세요."
                    )
                }
                println("Thief $thiefUserId confirmed caught, jail: ${jailLoc.latitude}, ${jailLoc.longitude}")
            } else {
                println("Error: No jail location available")
                _uiState.update { it.copy(error = "감옥 위치를 찾을 수 없습니다") }
            }
        }
    }

    // Thief: Reject catch request
    fun rejectCatch(thiefUserId: String) {
        viewModelScope.launch {
            wsClient.sendCatchRejected(thiefUserId)
            _uiState.update {
                it.copy(
                    catchRequest = null,
                    error = "체포를 거부했습니다"
                )
            }
            println("Thief $thiefUserId rejected catch")
        }
    }

    // Dismiss catch request dialog
    fun dismissCatchRequest() {
        _uiState.update { it.copy(catchRequest = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun leaveGame() {
        println("🚪 Leaving game: $gameId")
        stopLocationTracking()
        viewModelScope.launch {
            // Call server API to leave game
            gameRepository.leaveGame(gameId)
                .onSuccess {
                    println("✅ Successfully left game")
                }
                .onFailure { error ->
                    println("❌ Failed to leave game: ${error.message}")
                }

            // Disconnect WebSocket
            wsClient.disconnect()

            // Reset UI state to initial values
            _uiState.update {
                GameUiState(
                    gameId = gameId,
                    myRole = myRole,
                    startTime = startTime,
                    escapeDurationSeconds = escapeDurationSeconds,
                    totalDurationSeconds = totalDurationSeconds,
                    connectionState = GameWebSocketClient.ConnectionState.DISCONNECTED
                )
            }
            println("🔄 Game state reset to initial values")
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationTracking()
        viewModelScope.launch {
            wsClient.disconnect()
        }
    }
}
