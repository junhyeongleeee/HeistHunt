package com.heisthunt.app.voice

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.webrtc.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual class VoiceChannelManager(private val context: Context) {
    private var factory: PeerConnectionFactory? = null
    private var localAudioTrack: AudioTrack? = null
    private val peerConnections = mutableMapOf<String, PeerConnection>()
    private val iceCandidateQueues = mutableMapOf<String, MutableList<IceCandidate>>()
    private var myUserId: String = ""

    private val _connectionState = MutableStateFlow(VoiceConnectionState.IDLE)
    actual val connectionState: StateFlow<VoiceConnectionState> = _connectionState

    private val _peerStates = MutableStateFlow(emptyMap<String, PeerAudioState>())
    actual val peerStates: StateFlow<Map<String, PeerAudioState>> = _peerStates

    private val _isMuted = MutableStateFlow(false)
    actual val isMuted: StateFlow<Boolean> = _isMuted

    actual var onLocalIceCandidate: ((toUserId: String, sdp: String, sdpMLineIndex: Int, sdpMid: String?) -> Unit)? = null

    actual fun initialize(myUserId: String) {
        this.myUserId = myUserId
        println("🎤 [VoiceChannelManager] Initializing for user: $myUserId")

        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions
                .builder(context)
                .createInitializationOptions()
        )

        factory = PeerConnectionFactory.builder().createPeerConnectionFactory()

        val audioSource = factory!!.createAudioSource(MediaConstraints())
        localAudioTrack = factory!!.createAudioTrack("LOCAL_AUDIO_TRACK", audioSource)
        localAudioTrack?.setEnabled(true)

        _connectionState.value = VoiceConnectionState.CONNECTING
        println("✅ [VoiceChannelManager] Initialized successfully")
    }

    private fun buildPeerConnectionFor(peerId: String): PeerConnection? {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        val observer = object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate ?: return
                println("🧊 [VoiceChannelManager] New ICE candidate for $peerId")
                onLocalIceCandidate?.invoke(peerId, candidate.sdp, candidate.sdpMLineIndex, candidate.sdpMid)
            }
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                println("🔌 [VoiceChannelManager] ICE connection state for $peerId: $state")
                val status = when (state) {
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED -> PeerConnectionStatus.CONNECTED
                    PeerConnection.IceConnectionState.CHECKING -> PeerConnectionStatus.CONNECTING
                    else -> PeerConnectionStatus.DISCONNECTED
                }
                updatePeerStatus(peerId, status)
                if (peerConnections.isNotEmpty() && status == PeerConnectionStatus.CONNECTED) {
                    _connectionState.value = VoiceConnectionState.ACTIVE
                }
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                println("🎧 [VoiceChannelManager] Remote audio track added from $peerId")
            }
            override fun onTrack(transceiver: RtpTransceiver?) {}
        }

        val pc = factory?.createPeerConnection(rtcConfig, observer) ?: run {
            println("❌ [VoiceChannelManager] Failed to create PeerConnection for $peerId")
            return null
        }

        localAudioTrack?.let { pc.addTrack(it, listOf("LOCAL_STREAM")) }
        return pc
    }

    actual suspend fun createOfferFor(peerId: String): String? {
        println("📡 [VoiceChannelManager] Creating offer for $peerId")
        val pc = buildPeerConnectionFor(peerId) ?: return null
        peerConnections[peerId] = pc
        iceCandidateQueues[peerId] = mutableListOf()
        updatePeerStatus(peerId, PeerConnectionStatus.CONNECTING)

        return suspendCoroutine { cont ->
            val constraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            }
            pc.createOffer(object : SdpObserver {
                override fun onCreateSuccess(sdp: SessionDescription?) {
                    if (sdp == null) { cont.resume(null); return }
                    pc.setLocalDescription(object : SdpObserver {
                        override fun onSetSuccess() {
                            println("✅ [VoiceChannelManager] Offer created for $peerId")
                            cont.resume(sdp.description)
                        }
                        override fun onSetFailure(e: String?) { cont.resume(null) }
                        override fun onCreateSuccess(p: SessionDescription?) {}
                        override fun onCreateFailure(e: String?) {}
                    }, sdp)
                }
                override fun onCreateFailure(e: String?) {
                    println("❌ [VoiceChannelManager] createOffer failed: $e")
                    cont.resume(null)
                }
                override fun onSetSuccess() {}
                override fun onSetFailure(p: String?) {}
            }, constraints)
        }
    }

    actual suspend fun handleOffer(fromUserId: String, sdp: String): String? {
        println("📡 [VoiceChannelManager] Handling offer from $fromUserId")
        val pc = buildPeerConnectionFor(fromUserId) ?: return null
        peerConnections[fromUserId] = pc
        iceCandidateQueues[fromUserId] = mutableListOf()
        updatePeerStatus(fromUserId, PeerConnectionStatus.CONNECTING)

        return suspendCoroutine { cont ->
            pc.setRemoteDescription(object : SdpObserver {
                override fun onSetSuccess() {
                    // Flush queued ICE candidates
                    iceCandidateQueues[fromUserId]?.forEach { pc.addIceCandidate(it) }
                    iceCandidateQueues.remove(fromUserId)

                    val constraints = MediaConstraints().apply {
                        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                    }
                    pc.createAnswer(object : SdpObserver {
                        override fun onCreateSuccess(answerSdp: SessionDescription?) {
                            if (answerSdp == null) { cont.resume(null); return }
                            pc.setLocalDescription(object : SdpObserver {
                                override fun onSetSuccess() {
                                    println("✅ [VoiceChannelManager] Answer created for $fromUserId")
                                    cont.resume(answerSdp.description)
                                }
                                override fun onSetFailure(e: String?) { cont.resume(null) }
                                override fun onCreateSuccess(p: SessionDescription?) {}
                                override fun onCreateFailure(e: String?) {}
                            }, answerSdp)
                        }
                        override fun onCreateFailure(e: String?) {
                            println("❌ [VoiceChannelManager] createAnswer failed: $e")
                            cont.resume(null)
                        }
                        override fun onSetSuccess() {}
                        override fun onSetFailure(p: String?) {}
                    }, constraints)
                }
                override fun onSetFailure(e: String?) {
                    println("❌ [VoiceChannelManager] setRemoteDescription (offer) failed: $e")
                    cont.resume(null)
                }
                override fun onCreateSuccess(p: SessionDescription?) {}
                override fun onCreateFailure(p: String?) {}
            }, SessionDescription(SessionDescription.Type.OFFER, sdp))
        }
    }

    actual suspend fun handleAnswer(fromUserId: String, sdp: String) {
        println("📡 [VoiceChannelManager] Handling answer from $fromUserId")
        val pc = peerConnections[fromUserId] ?: run {
            println("❌ [VoiceChannelManager] No PeerConnection for $fromUserId")
            return
        }
        suspendCoroutine<Unit> { cont ->
            pc.setRemoteDescription(object : SdpObserver {
                override fun onSetSuccess() {
                    println("✅ [VoiceChannelManager] Remote answer set for $fromUserId")
                    iceCandidateQueues[fromUserId]?.forEach { pc.addIceCandidate(it) }
                    iceCandidateQueues.remove(fromUserId)
                    cont.resume(Unit)
                }
                override fun onSetFailure(e: String?) {
                    println("❌ [VoiceChannelManager] setRemoteDescription (answer) failed: $e")
                    cont.resume(Unit)
                }
                override fun onCreateSuccess(p: SessionDescription?) {}
                override fun onCreateFailure(p: String?) {}
            }, SessionDescription(SessionDescription.Type.ANSWER, sdp))
        }
    }

    actual fun addIceCandidate(fromUserId: String, sdp: String, sdpMLineIndex: Int, sdpMid: String?) {
        val pc = peerConnections[fromUserId]
        val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
        if (pc?.remoteDescription != null) {
            pc.addIceCandidate(candidate)
            println("🧊 [VoiceChannelManager] Added ICE candidate from $fromUserId")
        } else {
            iceCandidateQueues.getOrPut(fromUserId) { mutableListOf() }.add(candidate)
            println("🧊 [VoiceChannelManager] Queued ICE candidate from $fromUserId (no remote desc yet)")
        }
    }

    actual fun removePeer(userId: String) {
        peerConnections[userId]?.close()
        peerConnections.remove(userId)
        iceCandidateQueues.remove(userId)
        _peerStates.value = _peerStates.value.toMutableMap().also { it.remove(userId) }
        if (peerConnections.isEmpty()) _connectionState.value = VoiceConnectionState.IDLE
        println("🔌 [VoiceChannelManager] Removed peer: $userId")
    }

    actual fun setPeerNickname(userId: String, nickname: String) {
        _peerStates.value = _peerStates.value.toMutableMap().also { map ->
            val existing = map[userId] ?: PeerAudioState(userId, nickname)
            map[userId] = existing.copy(nickname = nickname)
        }
    }

    actual fun toggleMute(): Boolean {
        val newMuted = !_isMuted.value
        _isMuted.value = newMuted
        localAudioTrack?.setEnabled(!newMuted)
        println("🎤 [VoiceChannelManager] Microphone ${if (newMuted) "MUTED" else "UNMUTED"}")
        return newMuted
    }

    actual fun dispose() {
        println("🔌 [VoiceChannelManager] Disposing all resources")
        peerConnections.values.forEach { it.close() }
        peerConnections.clear()
        iceCandidateQueues.clear()
        localAudioTrack?.dispose()
        localAudioTrack = null
        factory?.dispose()
        factory = null
        _connectionState.value = VoiceConnectionState.IDLE
        _peerStates.value = emptyMap()
    }

    private fun updatePeerStatus(userId: String, status: PeerConnectionStatus) {
        _peerStates.value = _peerStates.value.toMutableMap().also { map ->
            val existing = map[userId] ?: PeerAudioState(userId, userId)
            map[userId] = existing.copy(connectionStatus = status)
        }
    }
}
