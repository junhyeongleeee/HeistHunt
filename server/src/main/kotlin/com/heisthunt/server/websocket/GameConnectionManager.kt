package com.heisthunt.server.websocket

import com.heisthunt.server.database.Games
import com.heisthunt.shared.dto.WebSocketMessage
import com.heisthunt.shared.models.Location
import com.heisthunt.shared.models.PlayerLocation
import com.heisthunt.shared.models.PlayerRole
import com.heisthunt.shared.utils.GeoUtils
import io.ktor.websocket.*
import io.ktor.server.websocket.WebSocketServerSession
import kotlinx.coroutines.Job
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

    // gameId -> Job (timer jobs for game timeout)
    private val gameTimerJobs = ConcurrentHashMap<String, Job>()

    // gameId -> Job (phase transition timer)
    private val phaseTimerJobs = ConcurrentHashMap<String, Job>()

    // gameId -> set of caught userId (excluded from location broadcasts)
    private val caughtPlayers = ConcurrentHashMap<String, MutableSet<String>>()

    // gameId -> current phase (cached to avoid DB queries per broadcast)
    private val gamePhases = ConcurrentHashMap<String, String>()

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
            caughtPlayers.remove(gameId)
            gamePhases.remove(gameId)
            cancelGameTimer(gameId)
        }
    }

    fun registerGameTimer(gameId: String, job: Job) {
        gameTimerJobs[gameId] = job
        println("⏰ [GameTimer] Timer registered for game $gameId")
    }

    fun updateGamePhase(gameId: String, phase: String) {
        gamePhases[gameId] = phase
        println("🔄 [GameConnectionManager] Phase updated: gameId=$gameId, phase=$phase")
    }

    fun registerPhaseTimer(gameId: String, job: Job) {
        phaseTimerJobs[gameId] = job
        println("⏰ [PhaseTimer] Phase timer registered for game $gameId")
    }

    fun cancelGameTimer(gameId: String) {
        gameTimerJobs[gameId]?.cancel()
        gameTimerJobs.remove(gameId)
        phaseTimerJobs[gameId]?.cancel()
        phaseTimerJobs.remove(gameId)
        println("⏰ [GameTimer] All timers cancelled for game $gameId")
    }

    fun markPlayerCaught(gameId: String, userId: String) {
        caughtPlayers.getOrPut(gameId) { ConcurrentHashMap.newKeySet() }.add(userId)
        lastKnownLocations[gameId]?.remove(userId)
        println("🔒 [GameConnectionManager] Player $userId marked as caught in game $gameId")
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

        // Use cached phase (updated on game start and phase transitions) - avoids DB query per broadcast
        val currentPhase = gamePhases[gameId] ?: "ESCAPE"

        val deadConnections = mutableListOf<String>()
        connections[gameId]?.forEach { (receiverId, session) ->
            try {
                val receiverRole = playerRoles[gameId]?.get(receiverId) ?: return@forEach

                // Filter locations based on receiver's role and game phase
                // Always exclude: (1) receiver's own location, (2) caught thieves
                val caughtSet = caughtPlayers[gameId] ?: emptySet()
                val visibleLocations = if (currentPhase == "ESCAPE") {
                    // ESCAPE phase: Everyone can see everyone (except self and caught thieves)
                    allLocations.filter { it.userId != receiverId && !caughtSet.contains(it.userId) }
                } else {
                    // CHASE phase: Same-role visibility only (spec: police see police, thief see thief)
                    when (receiverRole) {
                        PlayerRole.POLICE -> allLocations.filter { it.role == PlayerRole.POLICE && it.userId != receiverId }
                        PlayerRole.THIEF -> allLocations.filter { it.role == PlayerRole.THIEF && it.userId != receiverId && !caughtSet.contains(it.userId) }
                    }
                }

                val message = WebSocketMessage.PlayerLocations(visibleLocations)
                val json = Json.encodeToString<WebSocketMessage>(message)
                session.send(Frame.Text(json))
                println("Broadcasted ${visibleLocations.size} locations to user $receiverId (role: $receiverRole, phase: $currentPhase)")
            } catch (e: Exception) {
                println("Error broadcasting to user $receiverId: ${e.message}")
                deadConnections.add(receiverId)
            }
        }
        deadConnections.forEach { removeConnection(gameId, it) }
    }

    suspend fun broadcastMessage(gameId: String, message: WebSocketMessage) {
        val json = Json.encodeToString(message)
        val deadConnections = mutableListOf<String>()
        connections[gameId]?.forEach { (userId, session) ->
            try {
                session.send(Frame.Text(json))
                println("Broadcasted message to user $userId: ${message::class.simpleName}")
            } catch (e: Exception) {
                println("Error broadcasting to user $userId: ${e.message}")
                deadConnections.add(userId)
            }
        }
        deadConnections.forEach { removeConnection(gameId, it) }
    }

    suspend fun broadcastToRole(
        gameId: String,
        targetRole: PlayerRole,
        message: WebSocketMessage
    ) {
        val json = Json.encodeToString(message)
        val deadConnections = mutableListOf<String>()
        connections[gameId]?.forEach { (userId, session) ->
            val userRole = playerRoles[gameId]?.get(userId)
            if (userRole == targetRole) {
                try {
                    session.send(Frame.Text(json))
                    println("Broadcasted message to user $userId (role: $targetRole): ${message::class.simpleName}")
                } catch (e: Exception) {
                    println("Error broadcasting to user $userId: ${e.message}")
                    deadConnections.add(userId)
                }
            }
        }
        deadConnections.forEach { removeConnection(gameId, it) }
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

    // CHASE 페이즈에서 발신자와 5m 이내 적 감지 후 EnemyProximityAlert 전송
    suspend fun sendProximityAlerts(gameId: String, senderId: String) {
        val senderRole = playerRoles[gameId]?.get(senderId) ?: return
        val senderLoc = lastKnownLocations[gameId]?.get(senderId)?.location ?: return
        val caughtSet = caughtPlayers[gameId] ?: emptySet()

        val enemyRole = if (senderRole == PlayerRole.POLICE) PlayerRole.THIEF else PlayerRole.POLICE

        val nearbyCount = lastKnownLocations[gameId]?.values
            ?.filter { it.role == enemyRole && !caughtSet.contains(it.userId) }
            ?.count { enemy ->
                GeoUtils.calculateDistance(
                    senderLoc.latitude, senderLoc.longitude,
                    enemy.location.latitude, enemy.location.longitude
                ) <= 5.0
            } ?: 0

        println("🔍 [Proximity] gameId=$gameId, sender=$senderId (${senderRole.name}), nearbyEnemies=$nearbyCount")

        // 항상 전송: 클라이언트가 0이면 alert 제거, 0 초과면 alert 표시
        sendToUser(
            gameId, senderId,
            WebSocketMessage.EnemyProximityAlert(enemyRole = enemyRole, count = nearbyCount)
        )
    }

    fun getConnectionCount(gameId: String): Int {
        return connections[gameId]?.size ?: 0
    }
}
