package com.heisthunt.app.network

import com.heisthunt.shared.dto.WebSocketMessage
import com.heisthunt.shared.models.Location
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class GameWebSocketClient(
    private val baseUrl: String,
    private val getAccessToken: () -> String?
) {
    private val client = HttpClient {
        install(WebSockets)
    }

    private var session: DefaultClientWebSocketSession? = null
    private val _events = MutableSharedFlow<WebSocketMessage>(replay = 0, extraBufferCapacity = 10)
    val events: SharedFlow<WebSocketMessage> = _events.asSharedFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var shouldReconnect = false
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private var currentGameId: String? = null

    suspend fun connect(gameId: String) {
        currentGameId = gameId
        shouldReconnect = true
        reconnectAttempts = 0
        connectInternal(gameId)
    }

    private suspend fun connectInternal(gameId: String) {
        if (session != null) {
            println("Game WebSocket already connected")
            return
        }

        val token = getAccessToken()
        if (token == null) {
            println("No access token available")
            _connectionState.value = ConnectionState.ERROR
            return
        }

        try {
            println("Connecting to Game WebSocket: ws://$baseUrl/api/games/ws/$gameId?token=***")
            _connectionState.value = ConnectionState.CONNECTING

            session = client.webSocketSession {
                url {
                    protocol = URLProtocol.WS
                    host = baseUrl.removePrefix("http://").removePrefix("https://")
                    path("/api/games/ws/$gameId")
                    parameters.append("token", token)
                }
            }

            _connectionState.value = ConnectionState.CONNECTED
            reconnectAttempts = 0
            println("Game WebSocket connected successfully")

            // Listen for incoming messages
            session?.let { currentSession ->
                try {
                    for (frame in currentSession.incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                val text = frame.readText()
                                println("Received Game WebSocket message: $text")
                                try {
                                    val event = Json.decodeFromString<WebSocketMessage>(text)
                                    println("Parsed event: ${event::class.simpleName}")
                                    _events.emit(event)
                                } catch (e: Exception) {
                                    println("Error parsing Game WebSocket message: ${e.message}")
                                    e.printStackTrace()
                                }
                            }
                            is Frame.Close -> {
                                println("Game WebSocket closed by server")
                                break
                            }
                            else -> {
                                println("Received non-text frame: ${frame.frameType}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("Error in Game WebSocket receive loop: ${e.message}")
                    e.printStackTrace()
                } finally {
                    if (currentSession.isActive) {
                        currentSession.close()
                    }
                    session = null
                    _connectionState.value = ConnectionState.DISCONNECTED
                    println("Game WebSocket disconnected")

                    // Attempt reconnection with exponential backoff
                    if (shouldReconnect && reconnectAttempts < maxReconnectAttempts) {
                        reconnectAttempts++
                        val delayMs = (1000L * (1L shl reconnectAttempts)).coerceAtMost(30_000L)
                        println("🔄 [WebSocket] Reconnecting in ${delayMs}ms (attempt $reconnectAttempts/$maxReconnectAttempts)")
                        _connectionState.value = ConnectionState.RECONNECTING
                        delay(delayMs)
                        connectInternal(gameId)
                    } else if (reconnectAttempts >= maxReconnectAttempts) {
                        println("❌ [WebSocket] Max reconnect attempts reached, giving up")
                        _connectionState.value = ConnectionState.ERROR
                    }
                }
            }
        } catch (e: Exception) {
            println("Error connecting to Game WebSocket: ${e.message}")
            e.printStackTrace()
            session = null

            // Attempt reconnection on connection failure too
            if (shouldReconnect && reconnectAttempts < maxReconnectAttempts) {
                reconnectAttempts++
                val delayMs = (1000L * (1L shl reconnectAttempts)).coerceAtMost(30_000L)
                println("🔄 [WebSocket] Reconnecting after error in ${delayMs}ms (attempt $reconnectAttempts/$maxReconnectAttempts)")
                _connectionState.value = ConnectionState.RECONNECTING
                delay(delayMs)
                connectInternal(gameId)
            } else {
                _connectionState.value = ConnectionState.ERROR
            }
        }
    }

    suspend fun disconnect() {
        println("Disconnecting Game WebSocket...")
        shouldReconnect = false
        currentGameId = null
        session?.close()
        session = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    suspend fun sendLocation(location: Location) {
        try {
            val message = WebSocketMessage.LocationUpdate(location)
            val json = Json.encodeToString<WebSocketMessage>(message)
            session?.send(Frame.Text(json))
            println("Sent location update: lat=${location.latitude}, lon=${location.longitude}")
        } catch (e: Exception) {
            println("Error sending location: ${e.message}")
            e.printStackTrace()
        }
    }

    suspend fun sendCatchRequest(policeUserId: String, policeNickname: String, thiefUserId: String) {
        try {
            val message = WebSocketMessage.CatchRequest(policeUserId, policeNickname, thiefUserId)
            val json = Json.encodeToString<WebSocketMessage>(message)
            session?.send(Frame.Text(json))
            println("Sent catch request to thief $thiefUserId")
        } catch (e: Exception) {
            println("Error sending catch request: ${e.message}")
            e.printStackTrace()
        }
    }

    suspend fun sendCatchConfirmed(
        thiefUserId: String,
        thiefNickname: String,
        jailLatitude: Double,
        jailLongitude: Double
    ) {
        try {
            val message = WebSocketMessage.CatchConfirmed(
                thiefUserId = thiefUserId,
                thiefNickname = thiefNickname,
                jailLatitude = jailLatitude,
                jailLongitude = jailLongitude
            )
            val json = Json.encodeToString<WebSocketMessage>(message)
            session?.send(Frame.Text(json))
            println("Sent catch confirmed for thief $thiefUserId")
        } catch (e: Exception) {
            println("Error sending catch confirmed: ${e.message}")
            e.printStackTrace()
        }
    }

    suspend fun sendCatchRejected(thiefUserId: String, reason: String = "도둑이 체포를 거부했습니다") {
        try {
            val message = WebSocketMessage.CatchRejected(thiefUserId, reason)
            val json = Json.encodeToString<WebSocketMessage>(message)
            session?.send(Frame.Text(json))
            println("Sent catch rejected for thief $thiefUserId")
        } catch (e: Exception) {
            println("Error sending catch rejected: ${e.message}")
            e.printStackTrace()
        }
    }

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        RECONNECTING,
        ERROR
    }
}
