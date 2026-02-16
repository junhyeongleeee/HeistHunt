package com.heisthunt.app.network

import com.heisthunt.shared.dto.RoomAction
import com.heisthunt.shared.dto.RoomEvent
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class RoomWebSocketClient(
    private val baseUrl: String,
    private val getAccessToken: () -> String?
) {
    private val client = HttpClient {
        install(WebSockets)
    }

    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        classDiscriminator = "type"
    }

    private var session: DefaultClientWebSocketSession? = null
    private val _events = MutableSharedFlow<RoomEvent>(replay = 0, extraBufferCapacity = 10)
    val events: SharedFlow<RoomEvent> = _events.asSharedFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    suspend fun connect(roomId: String) {
        if (session != null) {
            println("WebSocket already connected")
            return
        }

        val token = getAccessToken()
        if (token == null) {
            println("No access token available")
            _connectionState.value = ConnectionState.ERROR
            return
        }

        try {
            println("Connecting to WebSocket: ws://$baseUrl/api/rooms/$roomId/ws?token=***")
            _connectionState.value = ConnectionState.CONNECTING

            session = client.webSocketSession {
                url {
                    protocol = URLProtocol.WS
                    host = baseUrl.removePrefix("http://").removePrefix("https://")
                    path("/api/rooms/$roomId/ws")
                    parameters.append("token", token)
                }
            }

            _connectionState.value = ConnectionState.CONNECTED
            println("WebSocket connected successfully")

            // Listen for incoming messages
            session?.let { currentSession ->
                try {
                    for (frame in currentSession.incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                val text = frame.readText()
                                println("Received WebSocket message: $text")
                                try {
                                    val event = json.decodeFromString<RoomEvent>(text)
                                    println("Parsed event: ${event::class.simpleName}")
                                    _events.emit(event)
                                } catch (e: Exception) {
                                    println("Error parsing WebSocket message: ${e.message}")
                                }
                            }
                            is Frame.Close -> {
                                println("WebSocket closed by server")
                                break
                            }
                            else -> {
                                println("Received non-text frame: ${frame.frameType}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("Error in WebSocket receive loop: ${e.message}")
                } finally {
                    if (currentSession.isActive) {
                        currentSession.close()
                    }
                    session = null
                    _connectionState.value = ConnectionState.DISCONNECTED
                    println("WebSocket disconnected")
                }
            }
        } catch (e: Exception) {
            println("Error connecting to WebSocket: ${e.message}")
            e.printStackTrace()
            session = null
            _connectionState.value = ConnectionState.ERROR
        }
    }

    suspend fun disconnect() {
        println("Disconnecting WebSocket...")
        session?.close()
        session = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    suspend fun send(action: RoomAction) {
        try {
            val jsonString = json.encodeToString<RoomAction>(action)
            session?.send(Frame.Text(jsonString))
            println("Sent WebSocket action: ${action::class.simpleName}")
        } catch (e: Exception) {
            println("Error sending WebSocket message: ${e.message}")
        }
    }

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR
    }
}
