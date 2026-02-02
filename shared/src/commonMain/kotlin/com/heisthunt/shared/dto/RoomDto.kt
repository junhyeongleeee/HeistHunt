package com.heisthunt.shared.dto

import com.heisthunt.shared.models.PlayerRole
import com.heisthunt.shared.models.Room
import com.heisthunt.shared.models.RoomSettings
import kotlinx.serialization.Serializable

@Serializable
data class CreateRoomRequest(
    val name: String,
    val settings: RoomSettings = RoomSettings()
)

@Serializable
data class JoinRoomRequest(
    val code: String,
    val password: String? = null
)

@Serializable
data class RoomListResponse(
    val rooms: List<RoomSummary>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int
)

@Serializable
data class RoomSummary(
    val id: String,
    val code: String,
    val name: String,
    val hostNickname: String,
    val currentPlayers: Int,
    val maxPlayers: Int,
    val hasPassword: Boolean,
    val status: String
)

@Serializable
data class UpdateRoomSettingsRequest(
    val settings: RoomSettings
)

@Serializable
data class SelectRoleRequest(
    val role: PlayerRole
)
