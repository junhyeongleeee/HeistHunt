package com.heisthunt.server.routes

import com.heisthunt.shared.dto.ApiResponse
import com.heisthunt.shared.dto.ErrorCodes
import com.heisthunt.shared.dto.ErrorResponse
import com.heisthunt.shared.dto.RoomEvent
import com.heisthunt.shared.dto.StartGameResponse
import com.heisthunt.shared.dto.WebSocketMessage
import com.heisthunt.shared.models.PlayerRole
import com.heisthunt.server.database.*
import com.heisthunt.server.websocket.GameConnectionManager
import com.heisthunt.server.websocket.RoomConnectionManager
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

fun Route.gameRoutes() {
    route("/games") {
        authenticate("auth-jwt") {
            // Get user's active game
            get("/my-active-game") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)

                val activeGame = transaction {
                    // Find active game for this user
                    val game = (Games innerJoin GamePlayers innerJoin Rooms)
                        .selectAll()
                        .where {
                            (GamePlayers.userId eq userId) and
                            (Games.status eq "IN_PROGRESS")
                        }
                        .orderBy(Games.startedAt, SortOrder.DESC)
                        .limit(1)
                        .singleOrNull()

                    if (game == null) {
                        return@transaction null
                    }

                    val gameId = game[Games.id]
                    val roomId = game[Games.roomId]
                    val playerRole = PlayerRole.valueOf(game[GamePlayers.role])
                    val startTime = game[Games.startedAt]
                    val phase = game[Games.phase]

                    // Get room settings
                    val room = Rooms.selectAll().where { Rooms.id eq roomId }.singleOrNull()
                    val gameDurationMinutes = room?.get(Rooms.gameDurationMinutes) ?: 30

                    com.heisthunt.shared.dto.ActiveGameResponse(
                        gameId = gameId,
                        roomId = roomId,
                        roomName = room?.get(Rooms.name) ?: "",
                        myRole = playerRole,
                        startTime = startTime,
                        escapeDurationSeconds = 300L, // 5 minutes
                        totalDurationSeconds = gameDurationMinutes * 60L,
                        phase = phase
                    )
                }

                if (activeGame != null) {
                    call.respond(HttpStatusCode.OK, com.heisthunt.shared.dto.ApiResponse(success = true, data = activeGame))
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        com.heisthunt.shared.dto.ApiResponse<Unit>(
                            success = false,
                            error = com.heisthunt.shared.dto.ErrorResponse(
                                com.heisthunt.shared.dto.ErrorCodes.GAME_NOT_FOUND,
                                "No active game"
                            )
                        )
                    )
                }
            }

            // Start game
            post("/{roomId}/start") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: return@post
                val roomId = call.parameters["roomId"] ?: return@post

                val result = transaction {
                    val room = Rooms.selectAll().where { Rooms.id eq roomId }.singleOrNull()
                        ?: return@transaction StartGameResult.RoomNotFound

                    // Check if user is host
                    if (room[Rooms.hostId] != userId) {
                        return@transaction StartGameResult.NotHost
                    }

                    // Get participants
                    val participants = RoomParticipants.selectAll()
                        .where { RoomParticipants.roomId eq roomId }
                        .toList()

                    if (participants.size < 2) {
                        return@transaction StartGameResult.NotEnoughPlayers
                    }

                    // Check if all ready
                    val allReady = participants.all { it[RoomParticipants.isReady] }
                    if (!allReady) {
                        return@transaction StartGameResult.NotAllReady
                    }

                    // Assign roles - respect selectedRole if set, otherwise random
                    val policeIds = participants
                        .filter { it[RoomParticipants.selectedRole] == PlayerRole.POLICE.name }
                        .map { it[RoomParticipants.userId] }

                    println("🎭 Role Assignment Debug:")
                    participants.forEach { p ->
                        println("  User ${p[RoomParticipants.userId]}: selectedRole=${p[RoomParticipants.selectedRole]}")
                    }
                    println("  Police IDs: $policeIds")

                    // Update room status
                    val now = Clock.System.now()
                    Rooms.update({ Rooms.id eq roomId }) {
                        it[status] = "PLAYING"
                        it[updatedAt] = now
                    }

                    // Update participant roles
                    participants.forEach { p ->
                        val role = if (policeIds.contains(p[RoomParticipants.userId])) PlayerRole.POLICE else PlayerRole.THIEF
                        println("  ✅ Assigning role ${role.name} to user ${p[RoomParticipants.userId]}")
                        RoomParticipants.update({
                            (RoomParticipants.roomId eq roomId) and (RoomParticipants.userId eq p[RoomParticipants.userId])
                        }) {
                            it[RoomParticipants.role] = role.name
                        }
                    }

                    // Create game record
                    val gameId = UUID.randomUUID().toString()
                    Games.insert {
                        it[id] = gameId
                        it[Games.roomId] = roomId
                        it[status] = "IN_PROGRESS"
                        it[phase] = "ESCAPE" // 도망 페이즈로 시작
                        it[startedAt] = now
                    }

                    // Create game players
                    participants.forEach { p ->
                        val role = if (policeIds.contains(p[RoomParticipants.userId])) PlayerRole.POLICE else PlayerRole.THIEF
                        GamePlayers.insert {
                            it[id] = UUID.randomUUID().toString()
                            it[GamePlayers.gameId] = gameId
                            it[GamePlayers.userId] = p[RoomParticipants.userId]
                            it[GamePlayers.role] = role.name
                        }
                    }

                    val myRole = if (policeIds.contains(userId)) PlayerRole.POLICE else PlayerRole.THIEF

                    StartGameResult.Success(
                        StartGameResponse(
                            gameId = gameId,
                            yourRole = myRole,
                            startedAt = now,
                            durationMinutes = room[Rooms.gameDurationMinutes]
                        )
                    )
                }

                when (result) {
                    is StartGameResult.Success -> {
                        val gameId = result.response.gameId
                        val startTime = result.response.startedAt
                        val totalDuration = result.response.durationMinutes * 60L

                        // Get role assignments
                        val roleAssignments = transaction {
                            RoomParticipants.selectAll()
                                .where { RoomParticipants.roomId eq roomId }
                                .associate { it[RoomParticipants.userId] to (it[RoomParticipants.role] ?: "THIEF") }
                        }
                        println("🎭 Broadcasting role assignments: $roleAssignments")

                        // Broadcast GameStarted event to all clients in the room
                        RoomConnectionManager.broadcast(
                            roomId,
                            RoomEvent.GameStarted(
                                gameId = gameId,
                                startTime = startTime,
                                escapeDurationSeconds = 300L, // 5 minutes
                                totalDurationSeconds = totalDuration,
                                roleAssignments = roleAssignments
                            )
                        )

                        // 5분 후 ESCAPE -> CHASE 페이즈 전환
                        CoroutineScope(Dispatchers.IO).launch {
                            delay(5 * 60 * 1000L) // 5분

                            // 게임이 아직 진행중인지 확인
                            val isStillInProgress = transaction {
                                Games.selectAll()
                                    .where { (Games.id eq gameId) and (Games.status eq "IN_PROGRESS") }
                                    .singleOrNull() != null
                            }

                            if (isStillInProgress) {
                                transaction {
                                    Games.update({ Games.id eq gameId }) {
                                        it[phase] = "CHASE"
                                        it[chaseStartedAt] = Clock.System.now()
                                    }
                                }

                                // 모든 플레이어에게 페이즈 변경 알림
                                GameConnectionManager.broadcastMessage(
                                    gameId,
                                    WebSocketMessage.PhaseChanged(
                                        phase = com.heisthunt.shared.models.GamePhase.CHASE,
                                        message = "도망 시간 종료! 추격이 시작됩니다!"
                                    )
                                )
                                println("Game $gameId: Phase changed to CHASE")
                            }
                        }

                        call.respond(ApiResponse(success = true, data = result.response))
                    }
                    StartGameResult.RoomNotFound -> call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, error = ErrorResponse(ErrorCodes.ROOM_NOT_FOUND, "Room not found"))
                    )
                    StartGameResult.NotHost -> call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse<Unit>(success = false, error = ErrorResponse(ErrorCodes.NOT_HOST, "Only host can start game"))
                    )
                    StartGameResult.NotEnoughPlayers -> call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, error = ErrorResponse("NOT_ENOUGH_PLAYERS", "Need at least 2 players"))
                    )
                    StartGameResult.NotAllReady -> call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, error = ErrorResponse("NOT_ALL_READY", "Not all players are ready"))
                    )
                }
            }

            // Leave game
            post("/{gameId}/leave") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)

                val gameId = call.parameters["gameId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, error = ErrorResponse(ErrorCodes.INTERNAL_ERROR, "Game ID required"))
                )

                println("🚪 User $userId leaving game $gameId")

                transaction {
                    // Remove player from game
                    GamePlayers.deleteWhere {
                        (GamePlayers.gameId eq gameId) and (GamePlayers.userId eq userId)
                    }

                    // Check remaining players
                    val remainingPlayers = GamePlayers.selectAll()
                        .where { GamePlayers.gameId eq gameId }
                        .count()

                    println("   Remaining players: $remainingPlayers")

                    // If no players left, end the game
                    if (remainingPlayers == 0L) {
                        Games.update({ Games.id eq gameId }) {
                            it[status] = "COMPLETED"
                            it[endedAt] = Clock.System.now()
                        }
                        println("   Game ended - no players remaining")
                    }
                }

                // Disconnect from WebSocket
                GameConnectionManager.removeConnection(gameId, userId)

                // Notify other players
                GameConnectionManager.broadcastMessage(
                    gameId,
                    WebSocketMessage.PlayerLeft(userId)
                )

                call.respond(ApiResponse(success = true, data = "Left game"))
            }

            // Get game status
            get("/{gameId}/status") {
                val gameId = call.parameters["gameId"] ?: return@get
                println("📊 GET /games/$gameId/status")

                val result = transaction {
                    val game = Games.selectAll().where { Games.id eq gameId }.singleOrNull()
                        ?: return@transaction null

                    val room = Rooms.selectAll().where { Rooms.id eq game[Games.roomId] }.singleOrNull()
                        ?: return@transaction null

                    val players = GamePlayers.selectAll().where { GamePlayers.gameId eq gameId }.toList()

                    val thieves = players.filter { it[GamePlayers.role] == "THIEF" }
                    val caughtThieves = thieves.filter { it[GamePlayers.isCaught] }

                    val startedAt = game[Games.startedAt]
                    val durationSeconds = room[Rooms.gameDurationMinutes] * 60L
                    val now = Clock.System.now()
                    val elapsedSeconds = (now - startedAt).inWholeSeconds
                    val remainingSeconds = (durationSeconds - elapsedSeconds).coerceAtLeast(0)

                    // Calculate escape time remaining (5 minutes = 300 seconds)
                    val escapeTimeRemaining = if (game[Games.phase] == "ESCAPE") {
                        val escapeElapsed = (now - startedAt).inWholeSeconds
                        (300L - escapeElapsed).coerceAtLeast(0)
                    } else {
                        0L
                    }

                    println("   Game started at: $startedAt")
                    println("   Current time: $now")
                    println("   Elapsed seconds: $elapsedSeconds")
                    println("   Phase: ${game[Games.phase]}")
                    println("   Escape time remaining: $escapeTimeRemaining")
                    println("   Remaining time: $remainingSeconds")

                    mapOf(
                        "gameId" to gameId,
                        "status" to game[Games.status],
                        "phase" to game[Games.phase],
                        "remainingTimeSeconds" to remainingSeconds,
                        "escapeTimeRemaining" to escapeTimeRemaining,
                        "remainingThieves" to (thieves.size - caughtThieves.size),
                        "totalThieves" to thieves.size,
                        "caughtPlayers" to caughtThieves.map { it[GamePlayers.userId] }
                    )
                }

                if (result != null) {
                    println("   ✅ Responding with game status")
                    call.respond(ApiResponse(success = true, data = result))
                } else {
                    println("   ❌ Game not found")
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, error = ErrorResponse(ErrorCodes.GAME_NOT_FOUND, "Game not found"))
                    )
                }
            }
        }

        // WebSocket for real-time game updates
        webSocket("/ws/{gameId}") {
            val gameId = call.parameters["gameId"] ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No game ID"))
            val token = call.request.queryParameters["token"]

            if (token == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No authentication token"))
                return@webSocket
            }

            // Extract userId from token (simplified - in production, verify JWT properly)
            val userId = try {
                com.auth0.jwt.JWT.decode(token).getClaim("userId").asString()
            } catch (e: Exception) {
                println("Invalid token: ${e.message}")
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
                return@webSocket
            }

            // Verify user is a participant in this game and get their role
            val playerRole = transaction {
                GamePlayers.selectAll()
                    .where { (GamePlayers.gameId eq gameId) and (GamePlayers.userId eq userId) }
                    .singleOrNull()
                    ?.let { PlayerRole.valueOf(it[GamePlayers.role]) }
            }

            if (playerRole == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "User not in game"))
                return@webSocket
            }

            println("WebSocket connected: gameId=$gameId, userId=$userId, role=$playerRole")

            // Register connection
            GameConnectionManager.addConnection(gameId, userId, this, playerRole)

            try {
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        println("Received WebSocket message from user $userId: $text")

                        try {
                            val message = Json.decodeFromString<WebSocketMessage>(text)
                            when (message) {
                                is WebSocketMessage.LocationUpdate -> {
                                    // Save to database
                                    transaction {
                                        LocationUpdates.insert {
                                            it[id] = UUID.randomUUID().toString()
                                            it[LocationUpdates.gameId] = gameId
                                            it[LocationUpdates.userId] = userId
                                            it[latitude] = message.location.latitude
                                            it[longitude] = message.location.longitude
                                            it[accuracy] = message.location.accuracy
                                            it[timestamp] = message.location.timestamp
                                        }

                                        // 처음 위치 업데이트 시 감옥 위치 저장 (도둑의 경우)
                                        val player = GamePlayers.selectAll()
                                            .where { (GamePlayers.gameId eq gameId) and (GamePlayers.userId eq userId) }
                                            .singleOrNull()

                                        if (player != null && player[GamePlayers.role] == "THIEF" &&
                                            player[GamePlayers.jailLatitude] == null) {
                                            GamePlayers.update({
                                                (GamePlayers.gameId eq gameId) and (GamePlayers.userId eq userId)
                                            }) {
                                                it[jailLatitude] = message.location.latitude
                                                it[jailLongitude] = message.location.longitude
                                            }
                                            println("Jail location saved for thief $userId: ${message.location.latitude}, ${message.location.longitude}")
                                        }
                                    }

                                    // Update cache and broadcast
                                    GameConnectionManager.updateLocation(gameId, userId, message.location)
                                    GameConnectionManager.broadcastLocations(gameId, userId)
                                }

                                is WebSocketMessage.CatchRequest -> {
                                    // 경찰이 도둑 체포 요청
                                    println("Catch request from police $userId to thief ${message.thiefUserId}")
                                    // 도둑에게만 체포 요청 전송
                                    GameConnectionManager.sendToUser(gameId, message.thiefUserId, message)
                                }

                                is WebSocketMessage.CatchConfirmed -> {
                                    // 도둑이 체포 확인
                                    println("Catch confirmed by thief $userId")

                                    val allCaught = transaction {
                                        // 도둑 상태 업데이트
                                        GamePlayers.update({
                                            (GamePlayers.gameId eq gameId) and (GamePlayers.userId eq userId)
                                        }) {
                                            it[isCaught] = true
                                            it[caughtAt] = Clock.System.now()
                                        }

                                        // 게임 종료 조건 체크
                                        val thieves = GamePlayers.selectAll()
                                            .where { (GamePlayers.gameId eq gameId) and (GamePlayers.role eq "THIEF") }
                                            .toList()

                                        thieves.all { it[GamePlayers.isCaught] }
                                    }

                                    if (allCaught) {
                                        // 모든 도둑이 잡힘 - 경찰 승리
                                        transaction {
                                            Games.update({ Games.id eq gameId }) {
                                                it[status] = "FINISHED"
                                                it[winner] = "POLICE"
                                                it[endedAt] = Clock.System.now()
                                            }
                                        }

                                        // 게임 종료 브로드캐스트
                                        GameConnectionManager.broadcastMessage(
                                            gameId,
                                            WebSocketMessage.GameEnded(com.heisthunt.shared.models.GameWinner.POLICE)
                                        )
                                        println("Game $gameId ended - POLICE wins (all thieves caught)")
                                    }

                                    // 모든 플레이어에게 체포 확정 알림
                                    GameConnectionManager.broadcastMessage(gameId, message)
                                }

                                is WebSocketMessage.CatchRejected -> {
                                    // 도둑이 체포 거부
                                    println("Catch rejected by thief $userId")
                                    // 모든 플레이어에게 거부 알림
                                    GameConnectionManager.broadcastMessage(gameId, message)
                                }

                                else -> {
                                    println("Unhandled message type: ${message::class.simpleName}")
                                }
                            }
                        } catch (e: Exception) {
                            println("Error processing WebSocket message: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                println("WebSocket error for user $userId: ${e.message}")
            } finally {
                GameConnectionManager.removeConnection(gameId, userId)
                println("WebSocket disconnected: gameId=$gameId, userId=$userId")
            }
        }
    }
}

private sealed class StartGameResult {
    data class Success(val response: StartGameResponse) : StartGameResult()
    data object RoomNotFound : StartGameResult()
    data object NotHost : StartGameResult()
    data object NotEnoughPlayers : StartGameResult()
    data object NotAllReady : StartGameResult()
}
