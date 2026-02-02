package com.heisthunt.server.websocket

import com.heisthunt.shared.dto.RoomEvent
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

object RoomConnectionManager {
    // roomId -> Set of WebSocketSessions
    private val connections = ConcurrentHashMap<String, MutableSet<WebSocketSession>>()

    // userId -> WebSocketSession (for finding user's session)
    private val userSessions = ConcurrentHashMap<String, WebSocketSession>()

    fun subscribe(roomId: String, userId: String, session: WebSocketSession) {
        connections.getOrPut(roomId) { ConcurrentHashMap.newKeySet() }.add(session)
        userSessions[userId] = session
        println("User $userId subscribed to room $roomId. Total connections: ${connections[roomId]?.size}")
    }

    fun unsubscribe(roomId: String, userId: String, session: WebSocketSession) {
        connections[roomId]?.remove(session)
        userSessions.remove(userId)

        // Clean up empty room connections
        if (connections[roomId]?.isEmpty() == true) {
            connections.remove(roomId)
        }
        println("User $userId unsubscribed from room $roomId. Remaining connections: ${connections[roomId]?.size ?: 0}")
    }

    suspend fun broadcast(roomId: String, event: RoomEvent) {
        val sessions = connections[roomId] ?: return
        val json = Json.encodeToString<RoomEvent>(event)

        println("Broadcasting to room $roomId: ${event::class.simpleName} to ${sessions.size} clients")

        val deadSessions = mutableSetOf<WebSocketSession>()

        sessions.forEach { session ->
            try {
                session.send(Frame.Text(json))
            } catch (e: ClosedSendChannelException) {
                println("Session closed, will remove: ${e.message}")
                deadSessions.add(session)
            } catch (e: Exception) {
                println("Error sending to session: ${e.message}")
                deadSessions.add(session)
            }
        }

        // Remove dead sessions
        deadSessions.forEach { session ->
            connections[roomId]?.remove(session)
        }
    }

    fun getConnectionCount(roomId: String): Int {
        return connections[roomId]?.size ?: 0
    }
}
