package com.heisthunt.app.voice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import kotlinx.coroutines.*
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
    private var scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var headsetReceiver: BroadcastReceiver? = null

    private val _connectionState = MutableStateFlow(VoiceConnectionState.IDLE)
    actual val connectionState: StateFlow<VoiceConnectionState> = _connectionState

    private val _peerStates = MutableStateFlow(emptyMap<String, PeerAudioState>())
    actual val peerStates: StateFlow<Map<String, PeerAudioState>> = _peerStates

    private val _isMuted = MutableStateFlow(false)
    actual val isMuted: StateFlow<Boolean> = _isMuted

    private val _isSpeaking = MutableStateFlow(false)
    actual val isSpeaking: StateFlow<Boolean> = _isSpeaking

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

        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        updateAudioRouting()

        // 이어폰 연결/해제 동적 감지
        headsetReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_HEADSET_PLUG) updateAudioRouting()
            }
        }
        @Suppress("UnspecifiedRegisterReceiverFlag")
        context.registerReceiver(headsetReceiver, IntentFilter(Intent.ACTION_HEADSET_PLUG))

        // 재연결 시 이전 scope 취소 후 새 scope 생성
        if (!scope.isActive) {
            scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        }

        _connectionState.value = VoiceConnectionState.CONNECTING
        println("✅ [VoiceChannelManager] Initialized successfully")
        startAudioLevelMonitoring()
    }

    private var statsDebugLogged = false

    private fun startAudioLevelMonitoring() {
        scope.launch {
            while (isActive) {
                delay(200)
                val connections = peerConnections.toMap()
                if (connections.isEmpty()) continue

                // 내 오디오 레벨: stream-webrtc-android 레거시 stats → "ssrc" + "audioInputLevel" (0-32768)
                connections.values.firstOrNull()?.getStats { report ->
                    if (!statsDebugLogged) {
                        statsDebugLogged = true
                        println("=== [VoiceChannelManager] ALL stat types ===")
                        report.statsMap.values.forEach { s ->
                            println("  type=${s.type}, ALL_keys=${s.members.keys}")
                        }
                    }
                    report.statsMap.values.forEach { stats ->
                        when (stats.type) {
                            "transport" -> {
                                val bytesSent = stats.members["bytesSent"]
                                val bytesReceived = stats.members["bytesReceived"]
                                val dtls = stats.members["dtlsState"]
                                println("📊 [Stats] transport: dtls=$dtls sent=$bytesSent rcv=$bytesReceived")
                            }
                            "media-source" -> {
                                val level = (stats.members["audioLevel"] as? Double) ?: 0.0
                                _isSpeaking.value = level > 0.02
                            }
                            "outbound-rtp" -> {
                                val audioLevel = stats.members["audioLevel"]
                                    ?: stats.members["totalAudioEnergy"]
                                if (audioLevel != null) {
                                    val level = (audioLevel as? Number)?.toDouble() ?: 0.0
                                    _isSpeaking.value = level > 0.02
                                }
                            }
                        }
                    }
                }

                // 상대방 오디오 레벨
                connections.forEach { (peerId, pc) ->
                    pc.getStats { report ->
                        report.statsMap.values.forEach { stats ->
                            when (stats.type) {
                                "inbound-rtp" -> {
                                    val audioLevel = stats.members["audioLevel"]
                                        ?: stats.members["totalAudioEnergy"]
                                    if (audioLevel != null) {
                                        val level = (audioLevel as? Number)?.toDouble() ?: 0.0
                                        val isTalking = level > 0.02
                                        val current = _peerStates.value[peerId]
                                        if (current != null && current.isSpeaking != isTalking) {
                                            _peerStates.value = _peerStates.value.toMutableMap().also { map ->
                                                map[peerId] = current.copy(isSpeaking = isTalking)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun buildPeerConnectionFor(peerId: String): PeerConnection? {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun.cloudflare.com:3478").createIceServer(),
            // TURN relay servers (AP isolation / symmetric NAT 우회)
            PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer(),
            PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer(),
            PeerConnection.IceServer.builder("turns:openrelay.metered.ca:443")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer(),
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

    private fun isHeadsetConnected(): Boolean {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return devices.any {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        }
    }

    private fun updateAudioRouting() {
        val headset = isHeadsetConnected()
        audioManager.isSpeakerphoneOn = !headset
        println("🔊 [VoiceChannelManager] Audio routing: ${if (headset) "HEADSET/BT" else "SPEAKER"}")
    }

    actual fun dispose() {
        println("🔌 [VoiceChannelManager] Disposing all resources")
        headsetReceiver?.let { context.unregisterReceiver(it) }
        headsetReceiver = null
        audioManager.isSpeakerphoneOn = false
        audioManager.mode = AudioManager.MODE_NORMAL
        scope.cancel()
        peerConnections.values.forEach { it.close() }
        peerConnections.clear()
        iceCandidateQueues.clear()
        localAudioTrack?.dispose()
        localAudioTrack = null
        factory?.dispose()
        factory = null
        _connectionState.value = VoiceConnectionState.IDLE
        _peerStates.value = emptyMap()
        _isSpeaking.value = false
    }

    private fun updatePeerStatus(userId: String, status: PeerConnectionStatus) {
        _peerStates.value = _peerStates.value.toMutableMap().also { map ->
            val existing = map[userId] ?: PeerAudioState(userId, userId)
            map[userId] = existing.copy(connectionStatus = status)
        }
    }
}
