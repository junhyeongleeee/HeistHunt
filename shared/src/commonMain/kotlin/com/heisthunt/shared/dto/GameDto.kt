package com.heisthunt.shared.dto

import com.heisthunt.shared.models.GameWinner
import com.heisthunt.shared.models.Location
import com.heisthunt.shared.models.PlayerLocation
import com.heisthunt.shared.models.PlayerRole
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class StartGameResponse(
    val gameId: String,
    val yourRole: PlayerRole,
    val startedAt: Instant,
    val durationMinutes: Int
)

@Serializable
data class GameStateResponse(
    val gameId: String,
    val status: String,
    val phase: String = "ESCAPE", // ESCAPE or CHASE
    val remainingTimeSeconds: Long, // 전체 게임 남은 시간
    val escapeTimeRemaining: Long = 0, // ESCAPE 페이즈 남은 시간 (초)
    val remainingThieves: Int,
    val totalThieves: Int,
    val caughtPlayers: List<String>
)

@Serializable
data class CatchRequest(
    val targetUserId: String
)

@Serializable
data class CatchResponse(
    val success: Boolean,
    val message: String
)

@Serializable
data class GameResultResponse(
    val gameId: String,
    val winner: GameWinner,
    val duration: Long,
    val players: List<PlayerResult>
)

@Serializable
data class PlayerResult(
    val userId: String,
    val nickname: String,
    val role: PlayerRole,
    val isCaught: Boolean
)

@Serializable
data class ActiveGameResponse(
    val gameId: String,
    val roomId: String,
    val roomName: String,
    val myRole: PlayerRole,
    val startTime: Instant,
    val escapeDurationSeconds: Long,
    val totalDurationSeconds: Long,
    val phase: String,
    val participants: List<com.heisthunt.shared.models.Participant> = emptyList()
)

// WebSocket Messages
@Serializable
sealed class WebSocketMessage {
    @Serializable
    data class LocationUpdate(val location: Location) : WebSocketMessage()

    @Serializable
    data class PlayerLocations(val locations: List<PlayerLocation>) : WebSocketMessage()

    @Serializable
    data class CatchRequest(
        val policeUserId: String,
        val policeNickname: String,
        val thiefUserId: String
    ) : WebSocketMessage()

    @Serializable
    data class CatchConfirmed(
        val thiefUserId: String,
        val thiefNickname: String,
        val jailLatitude: Double,
        val jailLongitude: Double
    ) : WebSocketMessage()

    @Serializable
    data class CatchRejected(
        val thiefUserId: String,
        val reason: String = "도둑이 체포를 거부했습니다"
    ) : WebSocketMessage()

    @Serializable
    data class PlayerCaught(val userId: String, val nickname: String) : WebSocketMessage()

    @Serializable
    data class GameEnded(val winner: GameWinner) : WebSocketMessage()

    @Serializable
    data class PhaseChanged(
        val phase: com.heisthunt.shared.models.GamePhase,
        val message: String
    ) : WebSocketMessage()

    @Serializable
    data class PlayerLeft(val userId: String) : WebSocketMessage()

    @Serializable
    data class Error(val message: String) : WebSocketMessage()

    @Serializable
    data class PoliceViolation(
        val policeUserId: String,
        val distanceFromJail: Double
    ) : WebSocketMessage()

    @Serializable
    data class EnemyProximityAlert(
        val enemyRole: PlayerRole,
        val count: Int
    ) : WebSocketMessage()

    // WebRTC signaling messages (relayed via Game WebSocket server)
    @Serializable
    data class RTCOffer(
        val fromUserId: String,
        val toUserId: String,
        val sdp: String
    ) : WebSocketMessage()

    @Serializable
    data class RTCAnswer(
        val fromUserId: String,
        val toUserId: String,
        val sdp: String
    ) : WebSocketMessage()

    @Serializable
    data class RTCIceCandidate(
        val fromUserId: String,
        val toUserId: String,
        val sdp: String,
        val sdpMLineIndex: Int,
        val sdpMid: String? = null
    ) : WebSocketMessage()

    @Serializable
    data class RTCPeerLeft(
        val userId: String
    ) : WebSocketMessage()

    @Serializable
    data class ThiefBoundaryViolation(
        val thiefUserId: String,
        val thiefNickname: String,
        val warningCount: Int,
        val isSentToJail: Boolean = false
    ) : WebSocketMessage()
}
