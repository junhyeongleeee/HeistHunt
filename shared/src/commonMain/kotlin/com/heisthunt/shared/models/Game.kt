package com.heisthunt.shared.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Game(
    val id: String,
    val roomId: String,
    val status: GameStatus,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val winner: GameWinner? = null,
    val players: List<GamePlayer> = emptyList()
)

@Serializable
enum class GameStatus {
    STARTING,
    IN_PROGRESS,
    PAUSED,
    FINISHED
}

@Serializable
enum class GameWinner {
    POLICE,
    THIEF
}

@Serializable
enum class GamePhase {
    ESCAPE,  // 도망 시간 (5분) - 도둑이 숨는 시간
    CHASE    // 추격 시간 - 경찰이 도둑을 잡는 시간
}

@Serializable
data class GamePlayer(
    val userId: String,
    val nickname: String,
    val role: PlayerRole,
    val isCaught: Boolean = false,
    val caughtAt: Instant? = null
)

@Serializable
data class Location(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val timestamp: Instant
)

@Serializable
data class PlayerLocation(
    val userId: String,
    val location: Location,
    val role: PlayerRole,
    val lastUpdateTimestamp: Instant = kotlinx.datetime.Clock.System.now()
)
