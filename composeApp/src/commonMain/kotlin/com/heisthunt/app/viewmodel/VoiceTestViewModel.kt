package com.heisthunt.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heisthunt.app.di.getPlatformBaseUrl
import com.heisthunt.app.voice.VoiceChannelManager
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class VoiceTestViewModel(
    private val voiceChannelManager: VoiceChannelManager
) : ViewModel() {

    private val httpClient = HttpClient {
        install(WebSockets)
    }

    private val wsBaseUrl get() = getPlatformBaseUrl()
        .removePrefix("http://").removePrefix("https://")

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _myUserId = MutableStateFlow("")
    val myUserId: StateFlow<String> = _myUserId.asStateFlow()

    val peerStates = voiceChannelManager.peerStates
    val voiceConnectionState = voiceChannelManager.connectionState
    val isMuted = voiceChannelManager.isMuted
    val isSpeaking = voiceChannelManager.isSpeaking

    private var wsSession: DefaultClientWebSocketSession? = null

    fun connect(roomId: String, nickname: String) {
        val userId = nickname.trim().ifBlank { "user-${(1000..9999).random()}" }
        _myUserId.value = userId

        viewModelScope.launch {
            try {
                println("🎤 [VoiceTest] Connecting: userId=$userId, room=$roomId")
                voiceChannelManager.initialize(userId)
                voiceChannelManager.onLocalIceCandidate = { toUserId, sdp, index, mid ->
                    viewModelScope.launch {
                        sendRaw(buildIceMsg(userId, toUserId, sdp, index, mid))
                    }
                }

                httpClient.webSocket("ws://$wsBaseUrl/api/voice/test/$roomId?userId=$userId") {
                    wsSession = this
                    _isConnected.value = true
                    println("🔌 [VoiceTest] WebSocket connected to room $roomId")

                    // 입장 알림
                    sendRaw("""{"type":"hello","userId":"$userId","nickname":"${nickname.trim()}"}""")

                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            handleMessage(frame.readText(), userId)
                        }
                    }
                }
            } catch (e: Exception) {
                println("❌ [VoiceTest] Connection error: ${e.message}")
            } finally {
                _isConnected.value = false
                wsSession = null
                println("🔌 [VoiceTest] Disconnected from room")
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            wsSession?.close()
            voiceChannelManager.dispose()
        }
    }

    fun toggleMute(): Boolean = voiceChannelManager.toggleMute()

    private suspend fun handleMessage(text: String, myUserId: String) {
        println("📥 [VoiceTest] Received: ${text.take(100)}")
        try {
            val json = Json.parseToJsonElement(text).jsonObject
            when (json["type"]?.jsonPrimitive?.content) {
                "hello" -> {
                    val peerId = json["userId"]?.jsonPrimitive?.content ?: return
                    val peerNick = json["nickname"]?.jsonPrimitive?.content ?: peerId
                    if (peerId == myUserId) return
                    println("👋 [VoiceTest] Peer joined: $peerId ($peerNick)")
                    voiceChannelManager.setPeerNickname(peerId, peerNick)
                    // lower userId가 offer 발신
                    if (myUserId < peerId) {
                        viewModelScope.launch {
                            val sdp = voiceChannelManager.createOfferFor(peerId)
                            if (sdp != null) {
                                sendRaw(buildOfferMsg(myUserId, peerId, sdp))
                                println("📡 [VoiceTest] Sent offer to $peerId")
                            }
                        }
                    }
                }
                "offer" -> {
                    val fromId = json["fromUserId"]?.jsonPrimitive?.content ?: return
                    val sdp = json["sdp"]?.jsonPrimitive?.content ?: return
                    println("📡 [VoiceTest] Got offer from $fromId")
                    voiceChannelManager.setPeerNickname(fromId, fromId)
                    val answer = voiceChannelManager.handleOffer(fromId, sdp)
                    if (answer != null) {
                        sendRaw(buildAnswerMsg(myUserId, fromId, answer))
                        println("📡 [VoiceTest] Sent answer to $fromId")
                    }
                }
                "answer" -> {
                    val fromId = json["fromUserId"]?.jsonPrimitive?.content ?: return
                    val sdp = json["sdp"]?.jsonPrimitive?.content ?: return
                    println("📡 [VoiceTest] Got answer from $fromId")
                    voiceChannelManager.handleAnswer(fromId, sdp)
                }
                "ice" -> {
                    val fromId = json["fromUserId"]?.jsonPrimitive?.content ?: return
                    val sdp = json["sdp"]?.jsonPrimitive?.content ?: return
                    val index = json["sdpMLineIndex"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    val mid = json["sdpMid"]?.jsonPrimitive?.content
                    voiceChannelManager.addIceCandidate(fromId, sdp, index, mid)
                }
                "bye" -> {
                    val peerId = json["userId"]?.jsonPrimitive?.content ?: return
                    println("👋 [VoiceTest] Peer left: $peerId")
                    voiceChannelManager.removePeer(peerId)
                }
            }
        } catch (e: Exception) {
            println("❌ [VoiceTest] Error handling message: ${e.message}")
            println("  Raw: $text")
        }
    }

    private suspend fun sendRaw(text: String) {
        try {
            wsSession?.send(Frame.Text(text))
        } catch (e: Exception) {
            println("❌ [VoiceTest] Send failed: ${e.message}")
        }
    }

    private fun buildOfferMsg(from: String, to: String, sdp: String) =
        """{"type":"offer","fromUserId":"$from","toUserId":"$to","sdp":${JsonPrimitive(sdp)}}"""

    private fun buildAnswerMsg(from: String, to: String, sdp: String) =
        """{"type":"answer","fromUserId":"$from","toUserId":"$to","sdp":${JsonPrimitive(sdp)}}"""

    private fun buildIceMsg(from: String, to: String, sdp: String, index: Int, mid: String?) =
        """{"type":"ice","fromUserId":"$from","toUserId":"$to","sdp":${JsonPrimitive(sdp)},"sdpMLineIndex":$index,"sdpMid":${if (mid != null) JsonPrimitive(mid) else "null"}}"""

    override fun onCleared() {
        super.onCleared()
        httpClient.close()
        voiceChannelManager.dispose()
    }
}
