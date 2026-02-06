package com.heisthunt.shared.dto

import com.heisthunt.shared.models.Participant
import com.heisthunt.shared.models.PlayerRole
import com.heisthunt.shared.models.Room
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class RoomEvent {
    @Serializable
    @SerialName("participant_joined")
    data class ParticipantJoined(val participant: Participant) : RoomEvent()

    @Serializable
    @SerialName("participant_left")
    data class ParticipantLeft(val userId: String) : RoomEvent()

    @Serializable
    @SerialName("participant_ready")
    data class ParticipantReady(val userId: String, val isReady: Boolean) : RoomEvent()

    @Serializable
    @SerialName("role_selected")
    data class RoleSelected(val userId: String, val role: PlayerRole) : RoomEvent()

    @Serializable
    @SerialName("room_updated")
    data class RoomUpdated(val room: Room) : RoomEvent()

    @Serializable
    @SerialName("game_started")
    data class GameStarted(
        val gameId: String,
        val startTime: Instant,
        val escapeDurationSeconds: Long,
        val totalDurationSeconds: Long,
        val roleAssignments: Map<String, String> = emptyMap() // userId -> role name
    ) : RoomEvent()
}

@Serializable
sealed class RoomAction {
    @Serializable
    @SerialName("subscribe")
    data class Subscribe(val roomId: String) : RoomAction()

    @Serializable
    @SerialName("unsubscribe")
    data class Unsubscribe(val roomId: String) : RoomAction()

    @Serializable
    @SerialName("toggle_ready")
    object ToggleReady : RoomAction()
}
