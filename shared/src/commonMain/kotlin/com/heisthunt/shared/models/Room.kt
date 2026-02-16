package com.heisthunt.shared.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Room(
    val id: String,
    val code: String,
    val name: String,
    val hostId: String,
    val settings: RoomSettings,
    val status: RoomStatus,
    val participants: List<Participant> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class RoomSettings(
    val maxPlayers: Int = 10,
    val policeRatio: Float = 0.3f,
    val gameDurationMinutes: Int = 30,
    val escapeDurationMinutes: Int = 5,
    val password: String? = null
)

@Serializable
enum class RoomStatus {
    WAITING,
    READY,
    PLAYING,
    FINISHED
}

@Serializable
data class Participant(
    val userId: String,
    val nickname: String,
    val isReady: Boolean = false,
    val selectedRole: PlayerRole? = null, // 유저가 선택한 역할 (게임 시작 전)
    val role: PlayerRole? = null, // 게임 시작 시 할당된 역할
    val isCaught: Boolean = false,
    val joinedAt: Instant
)

@Serializable
enum class PlayerRole {
    POLICE,
    THIEF
}
