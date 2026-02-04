package com.heisthunt.server.websocket

import com.heisthunt.server.database.Games
import com.heisthunt.shared.dto.WebSocketMessage
import com.heisthunt.shared.models.Location
import com.heisthunt.shared.models.PlayerLocation
import com.heisthunt.shared.models.PlayerRole
import io.ktor.websocket.*
import io.ktor.server.websocket.WebSocketServerSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.ConcurrentHashMap

object GameConnectionManager {
    // gameId -> userId -> WebSocketServerSession
    private val connections = ConcurrentHashMap<String, ConcurrentHashMap<String, WebSocketServerSession>>()

    // gameId -> userId -> PlayerLocation (cache for last known locations)
    private val lastKnownLocations = ConcurrentHashMap<String, ConcurrentHashMap<String, PlayerLocation>>()

    // gameId -> userId -> PlayerRole (cache for player roles)
    private val playerRoles = ConcurrentHashMap<String, ConcurrentHashMap<String, PlayerRole>>()

    fun addConnection(gameId: String, userId: String, session: WebSocketServerSession, role: PlayerRole) {
        connections.getOrPut(gameId) { ConcurrentHashMap() }[userId] = session
        playerRoles.getOrPut(gameId) { ConcurrentHashMap() }[userId] = role
        println("Game connection added: gameId=$gameId, userId=$userId, role=$role")
    }

    fun removeConnection(gameId: String, userId: String) {
        connections[gameId]?.remove(userId)
        lastKnownLocations[gameId]?.remove(userId)
        playerRoles[gameId]?.remove(userId)
        println("Game connection removed: gameId=$gameId, userId=$userId")

        // Clean up empty game sessions
        if (connections[gameId]?.isEmpty() == true) {
            connections.remove(gameId)
            lastKnownLocations.remove(gameId)
            playerRoles.remove(gameId)
        }
    }

    fun updateLocation(gameId: String, userId: String, location: Location) {
        val role = playerRoles[gameId]?.get(userId) ?: return
        val now = kotlinx.datetime.Clock.System.now()
        val playerLocation = PlayerLocation(
            userId = userId,
            location = location,
            role = role,
            lastUpdateTimestamp = now
        )
        lastKnownLocations.getOrPut(gameId) { ConcurrentHashMap() }[userId] = playerLocation
        println("Location updated: gameId=$gameId, userId=$userId, lat=${location.latitude}, lon=${location.longitude}, timestamp=$now")
    }

    suspend fun broadcastLocations(gameId: String, senderId: String) {
        val senderRole = playerRoles[gameId]?.get(senderId) ?: return
        val allLocations = lastKnownLocations[gameId]?.values?.toList() ?: emptyList()

        // Get current game phase from database
        val currentPhase = transaction {
            Games.selectAll().where { Games.id eq gameId }
                .singleOrNull()
                ?.get(Games.phase)
        } ?: "ESCAPE"

        connections[gameId]?.forEach { (receiverId, session) ->
            try {
                val receiverRole = playerRoles[gameId]?.get(receiverId) ?: return@forEach

                // Filter locations based on receiver's role and game phase
                val visibleLocations = if (currentPhase == "ESCAPE") {
                    // ESCAPE phase: Everyone can see everyone
                    allLocations
                } else {
                    // CHASE phase: Use normal rules
                    when (receiverRole) {
                        PlayerRole.POLICE -> allLocations // Police can see everyone
                        PlayerRole.THIEF -> allLocations.filter { it.role == PlayerRole.THIEF } // Thieves only see other thieves
                    }
                }

                val message = WebSocketMessage.PlayerLocations(visibleLocations)
                val json = Json.encodeToString(message)
                session.send(Frame.Text(json))
                println("Broadcasted ${visibleLocations.size} locations to user $receiverId (role: $receiverRole, phase: $currentPhase)")
            } catch (e: Exception) {
                println("Error broadcasting to user $receiverId: ${e.message}")
                // Remove dead connection
                removeConnection(gameId, receiverId)
            }
        }
    }

    suspend fun broadcastMessage(gameId: String, message: WebSocketMessage) {
        val json = Json.encodeToString(message)
        connections[gameId]?.forEach { (userId, session) ->
            try {
                session.send(Frame.Text(json))
                println("Broadcasted message to user $userId: ${message::class.simpleName}")
            } catch (e: Exception) {
                println("Error broadcasting to user $userId: ${e.message}")
                removeConnection(gameId, userId)
            }
        }
    }

    suspend fun sendToUser(gameId: String, userId: String, message: WebSocketMessage) {
        val session = connections[gameId]?.get(userId)
        if (session != null) {
            try {
                val json = Json.encodeToString(message)
                session.send(Frame.Text(json))
                println("Sent message to user $userId: ${message::class.simpleName}")
            } catch (e: Exception) {
                println("Error sending to user $userId: ${e.message}")
                removeConnection(gameId, userId)
            }
        } else {
            println("User $userId not connected to game $gameId")
        }
    }

    fun getConnectionCount(gameId: String): Int {
        return connections[gameId]?.size ?: 0
    }
}
