package com.heisthunt.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heisthunt.app.location.LocationService
import com.heisthunt.app.network.GameWebSocketClient
import com.heisthunt.app.repository.GameRepository
import com.heisthunt.app.utils.HapticFeedback
import com.heisthunt.app.voice.PeerConnectionStatus
import com.heisthunt.app.voice.VoiceChannelManager
import kotlinx.coroutines.delay
import com.heisthunt.shared.dto.GameStateResponse
import com.heisthunt.shared.dto.WebSocketMessage
import com.heisthunt.shared.models.GameWinner
import com.heisthunt.shared.models.Location
import com.heisthunt.shared.models.Participant
import com.heisthunt.shared.models.PlayerLocation
import com.heisthunt.shared.models.PlayerRole
import com.heisthunt.shared.utils.GeoUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

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

data class ThiefBoundaryViolationState(
    val thiefUserId: String,
    val thiefNickname: String,
    val warningCount: Int,
    val isSentToJail: Boolean,
    val timestamp: kotlinx.datetime.Instant
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
    val nearbyThieves: List<PlayerLocation> = emptyList(), // For police: thieves within 5m (legacy)
    val catchableThieves: List<Participant> = emptyList(), // For police: all uncaught thieves
    val catchRequest: CatchRequestState? = null, // For thief: incoming catch request
    val jailLocation: Location? = null, // For caught thief: where to return
    val policeJailLocation: Location? = null, // Police starting position (shown on map)
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
    val nearbyEnemies: ProximityAlertState? = null, // For both in CHASE: enemies within 5m
    val thiefBoundaryViolation: ThiefBoundaryViolationState? = null, // Thief out of bounds
    // Game result
    val gameResult: com.heisthunt.shared.dto.GameResultResponse? = null,
    // All participants list (for player list panel)
    val allParticipants: List<Participant> = emptyList()
)

class GameViewModel(
    private val gameRepository: GameRepository,
    private val gameId: String,
    private val myRole: PlayerRole,
    private val participants: List<Participant>,
    private val myUserId: String,
    private val locationService: LocationService,
    private val wsClient: GameWebSocketClient,
    private val hapticFeedback: HapticFeedback,
    private val startTime: kotlinx.datetime.Instant?,
    private val escapeDurationSeconds: Long,
    private val totalDurationSeconds: Long,
    val voiceChannelManager: VoiceChannelManager? = null
) : ViewModel() {

    // Vibration throttling (3초 간격)
    private var lastVibrationTime = 0L

    private val _uiState = MutableStateFlow(
        GameUiState(
            gameId = gameId,
            myRole = myRole,
            startTime = startTime,
            escapeDurationSeconds = escapeDurationSeconds,
            totalDurationSeconds = totalDurationSeconds,
            catchableThieves = participants.filter { it.role == PlayerRole.THIEF && !it.isCaught },
            allParticipants = participants
        )
    )
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private fun shouldVibrate(): Boolean {
        val now = Clock.System.now().toEpochMilliseconds()
        return if (now - lastVibrationTime >= 3000) {
            lastVibrationTime = now
            true
        } else {
            println("🔇 [HapticFeedback] Vibration throttled (last: ${now - lastVibrationTime}ms ago)")
            false
        }
    }

    init {
        println("🎮 GameViewModel initialized: gameId=$gameId, myRole=$myRole")

        // Connect to WebSocket
        viewModelScope.launch {
            wsClient.connect(gameId)
        }

        // Initialize voice channel for same-role teammates
        joinVoiceChannel()

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

                    // Timer reached 0 but no GameEnded WebSocket received (e.g. on rejoin or server restart)
                    // Fetch game result to trigger end screen
                    if (totalRemaining == 0L && !_uiState.value.isGameEnded) {
                        println("⏰ [GameViewModel] Local timer expired, fetching game result (gameId=$gameId)")
                        stopLocationTracking()
                        gameRepository.getGameResult(gameId).onSuccess { result ->
                            println("✅ [GameViewModel] Game result fetched after timer expiry: winner=${result.winner}")
                            _uiState.update {
                                it.copy(
                                    isGameEnded = true,
                                    winner = result.winner,
                                    gameResult = result
                                )
                            }
                        }.onFailure { error ->
                            println("❌ [GameViewModel] Failed to fetch result after timer expiry: ${error.message}")
                            // Mark ended anyway with THIEF win (time expired = thieves survive)
                            _uiState.update {
                                it.copy(
                                    isGameEnded = true,
                                    winner = com.heisthunt.shared.models.GameWinner.THIEF
                                )
                            }
                        }
                        break
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

                // 도둑은 ESCAPE 페이즈에서 경찰의 첫 위치를 jail로 저장
                val currentState = _uiState.value
                val updatedJailLocation = if (myRole == PlayerRole.THIEF && currentState.policeJailLocation == null) {
                    event.locations.firstOrNull { it.role == PlayerRole.POLICE }?.location?.also {
                        println("🔒 [GameViewModel] Thief sees police jail location: ${it.latitude}, ${it.longitude}")
                    }
                } else {
                    currentState.policeJailLocation
                }

                _uiState.update {
                    it.copy(
                        playerLocations = event.locations,
                        disconnectedPlayerIds = disconnected,
                        policeJailLocation = updatedJailLocation ?: it.policeJailLocation
                    )
                }
                // Calculate nearby enemies
                updateNearbyThieves() // For police
                updateNearbyPolice()  // For thieves
            }
            is WebSocketMessage.PlayerCaught -> {
                println("Player caught: ${event.nickname} (${event.userId})")
                _uiState.update {
                    it.copy(
                        error = "${event.nickname} has been caught!",
                        catchableThieves = it.catchableThieves.filter { p -> p.userId != event.userId },
                        allParticipants = it.allParticipants.map { p ->
                            if (p.userId == event.userId) p.copy(isCaught = true) else p
                        }
                    )
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
                if (shouldVibrate()) {
                    hapticFeedback.medium()
                }
            }
            is WebSocketMessage.CatchConfirmed -> {
                // All players receive confirmation that a thief was caught
                println("✅ [GameViewModel] Thief ${event.thiefNickname} (${event.thiefUserId}) confirmed caught")
                println("  myUserId: $myUserId, myRole: $myRole")
                println("  Is me: ${event.thiefUserId == myUserId}")

                // Remove caught thief from catchable list immediately
                _uiState.update {
                    it.copy(
                        error = "${event.thiefNickname}가 체포되었습니다!",
                        catchableThieves = it.catchableThieves.filter { p -> p.userId != event.thiefUserId },
                        allParticipants = it.allParticipants.map { p ->
                            if (p.userId == event.thiefUserId) p.copy(isCaught = true) else p
                        }
                    )
                }

                // Only update isCaught and jailLocation for the thief who was actually caught
                if (myRole == PlayerRole.THIEF && event.thiefUserId == myUserId) {
                    println("🎯 [GameViewModel] I was caught! Updating isCaught=true and jailLocation")
                    _uiState.update {
                        it.copy(
                            isCaught = true,
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
                // error 상태로 쓰지 않음 - loadGameStatus()로 phase 반영
                loadGameStatus()
            }
            is WebSocketMessage.GameEnded -> {
                println("🏁 [GameViewModel] Game ended, winner: ${event.winner}")

                // Stop location tracking immediately
                stopLocationTracking()

                // Fetch game result from server
                viewModelScope.launch {
                    println("📡 [GameViewModel] Fetching game result for gameId=$gameId")
                    gameRepository.getGameResult(gameId).onSuccess { result ->
                        println("✅ [GameViewModel] Game result received:")
                        println("   Winner: ${result.winner}")
                        println("   Duration: ${result.duration}s")
                        println("   Players: ${result.players.size}")

                        _uiState.update {
                            it.copy(
                                isGameEnded = true,
                                winner = event.winner,
                                gameResult = result
                            )
                        }
                    }.onFailure { error ->
                        println("❌ [GameViewModel] Failed to fetch game result: ${error.message}")
                        // Still mark game as ended even if we fail to fetch result
                        _uiState.update {
                            it.copy(
                                isGameEnded = true,
                                winner = event.winner
                            )
                        }
                    }
                }

                if (shouldVibrate()) {
                    hapticFeedback.heavy()
                }
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
                if (shouldVibrate()) {
                    hapticFeedback.warning()
                }
                // 5초 후 자동 제거
                viewModelScope.launch {
                    kotlinx.coroutines.delay(5000L)
                    _uiState.update { it.copy(policeViolation = null) }
                }
            }
            is WebSocketMessage.EnemyProximityAlert -> {
                println("🚨 [GameViewModel] EnemyProximityAlert: ${event.count} ${event.enemyRole.name} within 5m")
                _uiState.update {
                    it.copy(
                        nearbyEnemies = if (event.count > 0) {
                            ProximityAlertState(
                                enemyRole = event.enemyRole,
                                count = event.count
                            )
                        } else null
                    )
                }
                if (event.count > 0 && (_uiState.value.nearbyEnemies?.count ?: 0) == 0) {
                    if (shouldVibrate()) hapticFeedback.light()
                }
            }
            is WebSocketMessage.ThiefBoundaryViolation -> {
                println("⚠️ [GameViewModel] ThiefBoundaryViolation: ${event.thiefNickname}, warning=${event.warningCount}, jail=${event.isSentToJail}")
                _uiState.update {
                    it.copy(
                        thiefBoundaryViolation = ThiefBoundaryViolationState(
                            thiefUserId = event.thiefUserId,
                            thiefNickname = event.thiefNickname,
                            warningCount = event.warningCount,
                            isSentToJail = event.isSentToJail,
                            timestamp = Clock.System.now()
                        )
                    )
                }
                if (shouldVibrate()) hapticFeedback.warning()
            }
            is WebSocketMessage.RTCOffer -> {
                println("📡 [Voice] Received RTCOffer from ${event.fromUserId}")
                viewModelScope.launch {
                    val answerSdp = voiceChannelManager?.handleOffer(event.fromUserId, event.sdp)
                    if (answerSdp != null) {
                        wsClient.sendRTCAnswer(myUserId, event.fromUserId, answerSdp)
                    }
                }
            }
            is WebSocketMessage.RTCAnswer -> {
                println("📡 [Voice] Received RTCAnswer from ${event.fromUserId}")
                viewModelScope.launch {
                    voiceChannelManager?.handleAnswer(event.fromUserId, event.sdp)
                }
            }
            is WebSocketMessage.RTCIceCandidate -> {
                println("🧊 [Voice] Received RTCIceCandidate from ${event.fromUserId}")
                voiceChannelManager?.addIceCandidate(event.fromUserId, event.sdp, event.sdpMLineIndex, event.sdpMid)
            }
            is WebSocketMessage.RTCPeerLeft -> {
                println("🔌 [Voice] Peer left: ${event.userId}")
                voiceChannelManager?.removePeer(event.userId)
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

            // Vibrate on first detection only
            if (nearby.isNotEmpty() && previousCount == 0) {
                if (shouldVibrate()) {
                    hapticFeedback.light()
                }
            }

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

            // Vibrate on first detection only
            if (nearby.isNotEmpty() && previousCount == 0) {
                if (shouldVibrate()) {
                    hapticFeedback.light()
                }
            }

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
            intervalMillis = 1000L,
            onLocationUpdate = { location ->
                println("Location updated: lat=${location.latitude}, lon=${location.longitude}")
                val currentState = _uiState.value
                // 경찰의 첫 위치를 감옥(jail)으로 표시
                val updatedJailLocation = if (myRole == PlayerRole.POLICE && currentState.policeJailLocation == null) {
                    println("🔒 [GameViewModel] Police jail location set: ${location.latitude}, ${location.longitude}")
                    location
                } else {
                    currentState.policeJailLocation
                }
                _uiState.update { it.copy(myLocation = location, policeJailLocation = updatedJailLocation, error = null) }

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

                        _uiState.update {
                            it.copy(
                                gameStatus = status,
                                catchableThieves = participants.filter { p ->
                                    p.role == PlayerRole.THIEF && p.userId !in status.caughtPlayers
                                }
                            )
                        }

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
                if (shouldVibrate()) {
                    hapticFeedback.heavy()
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

    fun dismissThiefBoundaryViolation() {
        _uiState.update { it.copy(thiefBoundaryViolation = null) }
    }

    fun leaveGame() {
        println("🚪 Leaving game: $gameId")
        stopLocationTracking()
        viewModelScope.launch {
            // WS 먼저 끊어서 leave 과정 중 에러 이벤트 수신 방지
            wsClient.disconnect()

            // Call server API to leave game
            gameRepository.leaveGame(gameId)
                .onSuccess {
                    println("✅ Successfully left game")
                }
                .onFailure { error ->
                    println("❌ Failed to leave game: ${error.message}")
                }

            // Disconnect voice channel
            voiceChannelManager?.dispose()

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
        voiceChannelManager?.dispose()
        viewModelScope.launch {
            wsClient.disconnect()
        }
    }

    private fun joinVoiceChannel() {
        val vm = voiceChannelManager ?: return
        val sameRoleTeammates = participants.filter { it.role == myRole && it.userId != myUserId }

        // 같은 역할 팀원이 없으면 음성 채널 초기화 불필요 (IDLE 유지)
        if (sameRoleTeammates.isEmpty()) {
            println("🎙️ [Voice] No same-role teammates, skipping voice channel init")
            return
        }

        println("🎙️ [Voice] Initializing voice channel: role=$myRole, myUserId=$myUserId, teammates=${sameRoleTeammates.size}")
        vm.initialize(myUserId)

        // ICE candidate callback → relay via WebSocket
        vm.onLocalIceCandidate = { toUserId, sdp, sdpMLineIndex, sdpMid ->
            viewModelScope.launch {
                wsClient.sendRTCIceCandidate(myUserId, toUserId, sdp, sdpMLineIndex, sdpMid)
            }
        }

        sameRoleTeammates.forEach { teammate ->
            vm.setPeerNickname(teammate.userId, teammate.nickname)
        }

        viewModelScope.launch {
            // WebSocket CONNECTED 상태까지 대기
            wsClient.connectionState.first { it == GameWebSocketClient.ConnectionState.CONNECTED }
            // 상대방 WebSocket 연결 대기 (레이스 컨디션 방지)
            delay(3000)
            println("🎙️ [Voice] WebSocket connected, sending RTC offers")

            sameRoleTeammates.forEach { teammate ->
                if (myUserId < teammate.userId) {
                    launch {
                        // 최대 6회 재시도 (5초 간격, 총 30초)
                        repeat(6) { attempt ->
                            val alreadyConnected = vm.peerStates.value[teammate.userId]
                                ?.connectionStatus == PeerConnectionStatus.CONNECTED
                            if (alreadyConnected) return@repeat

                            if (attempt > 0) {
                                println("🔄 [Voice] Retry $attempt: resending offer to ${teammate.userId}")
                                vm.removePeer(teammate.userId)
                                delay(5000)
                            }

                            val sdp = vm.createOfferFor(teammate.userId)
                            if (sdp != null) {
                                wsClient.sendRTCOffer(myUserId, teammate.userId, sdp)
                                println("📡 [Voice] Sent RTCOffer to ${teammate.userId} (attempt ${attempt + 1})")
                            }

                            if (attempt < 5) delay(5000)
                        }
                    }
                }
            }
        }
    }
}
