package com.heisthunt.app.voice

import kotlinx.coroutines.flow.StateFlow

enum class VoiceConnectionState { IDLE, CONNECTING, ACTIVE }
enum class PeerConnectionStatus { DISCONNECTED, CONNECTING, CONNECTED }

data class PeerAudioState(
    val userId: String,
    val nickname: String,
    val isRemoteMuted: Boolean = false,
    val connectionStatus: PeerConnectionStatus = PeerConnectionStatus.DISCONNECTED
)

expect class VoiceChannelManager {
    val connectionState: StateFlow<VoiceConnectionState>
    val peerStates: StateFlow<Map<String, PeerAudioState>>
    val isMuted: StateFlow<Boolean>
    var onLocalIceCandidate: ((toUserId: String, sdp: String, sdpMLineIndex: Int, sdpMid: String?) -> Unit)?

    fun initialize(myUserId: String)
    suspend fun createOfferFor(peerId: String): String?
    suspend fun handleOffer(fromUserId: String, sdp: String): String?
    suspend fun handleAnswer(fromUserId: String, sdp: String)
    fun addIceCandidate(fromUserId: String, sdp: String, sdpMLineIndex: Int, sdpMid: String?)
    fun removePeer(userId: String)
    fun setPeerNickname(userId: String, nickname: String)
    fun toggleMute(): Boolean
    fun dispose()
}
