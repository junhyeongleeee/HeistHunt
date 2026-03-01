package com.heisthunt.app.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// TODO: Full WebRTC implementation for iOS
// Add GoogleWebRTC or WebRTC-lib pod and implement using RTCPeerConnectionFactory
// Reference: https://webrtc.github.io/webrtc-org/native-code/ios/
actual class VoiceChannelManager {
    private val _connectionState = MutableStateFlow(VoiceConnectionState.IDLE)
    actual val connectionState: StateFlow<VoiceConnectionState> = _connectionState

    private val _peerStates = MutableStateFlow(emptyMap<String, PeerAudioState>())
    actual val peerStates: StateFlow<Map<String, PeerAudioState>> = _peerStates

    private val _isMuted = MutableStateFlow(false)
    actual val isMuted: StateFlow<Boolean> = _isMuted

    actual var onLocalIceCandidate: ((toUserId: String, sdp: String, sdpMLineIndex: Int, sdpMid: String?) -> Unit)? = null

    private var myUserId: String = ""
    private val peerNicknames = mutableMapOf<String, String>()

    actual fun initialize(myUserId: String) {
        this.myUserId = myUserId
        println("🎤 [VoiceChannelManager iOS] Initialized for $myUserId")
        println("⚠️ [VoiceChannelManager iOS] WebRTC audio not yet implemented on iOS")
        _connectionState.value = VoiceConnectionState.IDLE
    }

    actual suspend fun createOfferFor(peerId: String): String? {
        println("📡 [VoiceChannelManager iOS] createOfferFor $peerId - WebRTC not implemented on iOS")
        // Update UI state to show connecting
        updatePeerStatus(peerId, PeerConnectionStatus.CONNECTING)
        return null
    }

    actual suspend fun handleOffer(fromUserId: String, sdp: String): String? {
        println("📡 [VoiceChannelManager iOS] handleOffer from $fromUserId - WebRTC not implemented on iOS")
        updatePeerStatus(fromUserId, PeerConnectionStatus.CONNECTING)
        return null
    }

    actual suspend fun handleAnswer(fromUserId: String, sdp: String) {
        println("📡 [VoiceChannelManager iOS] handleAnswer from $fromUserId - WebRTC not implemented on iOS")
    }

    actual fun addIceCandidate(fromUserId: String, sdp: String, sdpMLineIndex: Int, sdpMid: String?) {
        println("🧊 [VoiceChannelManager iOS] addIceCandidate from $fromUserId - WebRTC not implemented on iOS")
    }

    actual fun removePeer(userId: String) {
        _peerStates.value = _peerStates.value.toMutableMap().also { it.remove(userId) }
        peerNicknames.remove(userId)
        if (_peerStates.value.isEmpty()) _connectionState.value = VoiceConnectionState.IDLE
        println("🔌 [VoiceChannelManager iOS] Removed peer: $userId")
    }

    actual fun setPeerNickname(userId: String, nickname: String) {
        peerNicknames[userId] = nickname
        _peerStates.value = _peerStates.value.toMutableMap().also { map ->
            val existing = map[userId] ?: PeerAudioState(userId, nickname)
            map[userId] = existing.copy(nickname = nickname)
        }
    }

    actual fun toggleMute(): Boolean {
        val newMuted = !_isMuted.value
        _isMuted.value = newMuted
        println("🎤 [VoiceChannelManager iOS] Microphone ${if (newMuted) "MUTED" else "UNMUTED"} (audio not active)")
        return newMuted
    }

    actual fun dispose() {
        println("🔌 [VoiceChannelManager iOS] Disposed")
        _connectionState.value = VoiceConnectionState.IDLE
        _peerStates.value = emptyMap()
        peerNicknames.clear()
    }

    private fun updatePeerStatus(userId: String, status: PeerConnectionStatus) {
        _peerStates.value = _peerStates.value.toMutableMap().also { map ->
            val nickname = peerNicknames[userId] ?: userId
            val existing = map[userId] ?: PeerAudioState(userId, nickname)
            map[userId] = existing.copy(connectionStatus = status)
        }
    }
}
