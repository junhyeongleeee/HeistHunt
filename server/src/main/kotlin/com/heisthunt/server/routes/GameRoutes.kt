package com.heisthunt.server.routes

import com.heisthunt.shared.dto.ApiResponse
import com.heisthunt.shared.dto.ErrorCodes
import com.heisthunt.shared.dto.ErrorResponse
import com.heisthunt.shared.dto.RoomEvent
import com.heisthunt.shared.dto.StartGameResponse
import com.heisthunt.shared.dto.WebSocketMessage
import com.heisthunt.shared.models.PlayerRole
import com.heisthunt.shared.utils.GeoUtils
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
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.heisthunt.server.plugins.JwtConfig
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

fun Route.gameRoutes(jwtConfig: JwtConfig) {
    route("/games") {
        authenticate("auth-jwt") {
            // Get user's active game
            get("/my-active-game") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)

                val result = transaction {
                    val game = (Games innerJoin GamePlayers innerJoin Rooms)
                        .selectAll()
                        .where {
                            (GamePlayers.userId eq userId) and
                            (Games.status eq "IN_PROGRESS")
                        }
                        .orderBy(Games.startedAt, SortOrder.DESC)
                        .limit(1)
                        .singleOrNull() ?: return@transaction ActiveGameResult.NotFound

                    val gameId = game[Games.id]
                    val roomId = game[Games.roomId]
                    val playerRole = PlayerRole.valueOf(game[GamePlayers.role])
                    val startTime = game[Games.startedAt]
                    val phase = game[Games.phase]

                    val room = Rooms.selectAll().where { Rooms.id eq roomId }.singleOrNull()
                    val gameDurationMinutes = room?.get(Rooms.gameDurationMinutes) ?: 30
                    val escapeDurationMinutes = room?.get(Rooms.escapeDurationMinutes) ?: 5
                    val totalDurationSeconds = gameDurationMinutes * 60L
                    val elapsedSeconds = (Clock.System.now() - startTime).inWholeSeconds

                    // Time expired: update DB inside transaction, handle side-effects outside
                    if (elapsedSeconds >= totalDurationSeconds) {
                        println("⏰ [ActiveGame] Game $gameId time expired (elapsed=${elapsedSeconds}s >= total=${totalDurationSeconds}s), auto-ending")
                        Games.update({ Games.id eq gameId }) {
                            it[status] = "FINISHED"
                            it[winner] = "THIEF"
                            it[endedAt] = Clock.System.now()
                        }
                        LocationUpdates.deleteWhere { LocationUpdates.gameId eq gameId }
                        return@transaction ActiveGameResult.Expired(gameId)
                    }

                    val gamePlayers = (GamePlayers innerJoin Users)
                        .selectAll()
                        .where { GamePlayers.gameId eq gameId }
                        .map { row ->
                            com.heisthunt.shared.models.Participant(
                                userId = row[GamePlayers.userId],
                                nickname = row[Users.nickname],
                                role = PlayerRole.valueOf(row[GamePlayers.role]),
                                isCaught = row[GamePlayers.isCaught],
                                joinedAt = startTime
                            )
                        }

                    println("🔄 [ActiveGame] gameId=$gameId, phase=$phase, elapsed=${elapsedSeconds}s/${totalDurationSeconds}s, participants=${gamePlayers.size}")
                    gamePlayers.forEach { p -> println("  - ${p.nickname}: role=${p.role}, isCaught=${p.isCaught}") }

                    ActiveGameResult.Found(
                        com.heisthunt.shared.dto.ActiveGameResponse(
                            gameId = gameId,
                            roomId = roomId,
                            roomName = room?.get(Rooms.name) ?: "",
                            myRole = playerRole,
                            startTime = startTime,
                            escapeDurationSeconds = escapeDurationMinutes * 60L,
                            totalDurationSeconds = gameDurationMinutes * 60L,
                            phase = phase,
                            participants = gamePlayers
                        )
                    )
                }

                when (result) {
                    is ActiveGameResult.Found -> {
                        call.respond(HttpStatusCode.OK, com.heisthunt.shared.dto.ApiResponse(success = true, data = result.response))
                    }
                    is ActiveGameResult.Expired -> {
                        // Broadcast game ended to any still-connected clients, then cleanup
                        println("🏁 [ActiveGame] Game ${result.gameId} auto-ended, broadcasting GameEnded")
                        GameConnectionManager.broadcastMessage(
                            result.gameId,
                            WebSocketMessage.GameEnded(com.heisthunt.shared.models.GameWinner.THIEF)
                        )
                        GameConnectionManager.cancelGameTimer(result.gameId)
                        call.respond(
                            HttpStatusCode.NotFound,
                            com.heisthunt.shared.dto.ApiResponse<Unit>(
                                success = false,
                                error = com.heisthunt.shared.dto.ErrorResponse(
                                    com.heisthunt.shared.dto.ErrorCodes.GAME_NOT_FOUND,
                                    "Game time expired"
                                )
                            )
                        )
                    }
                    ActiveGameResult.NotFound -> {
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

                    // Assign roles - respect selectedRole if set, otherwise assign random police
                    var policeIds = participants
                        .filter { it[RoomParticipants.selectedRole] == PlayerRole.POLICE.name }
                        .map { it[RoomParticipants.userId] }

                    // Guarantee at least 1 police - use policeRatio from room settings if none selected
                    if (policeIds.isEmpty()) {
                        val policeRatio = room[Rooms.policeRatio]
                        val policeCount = (participants.size * policeRatio).toInt().coerceIn(1, participants.size - 1)
                        policeIds = participants.shuffled().take(policeCount).map { it[RoomParticipants.userId] }
                        println("⚠️ No one selected POLICE role - auto-assigning $policeCount police (ratio: $policeRatio)")
                    }

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

                        // Get room settings for escape duration
                        val escapeDurationSeconds = transaction {
                            val room = Rooms.selectAll().where { Rooms.id eq roomId }.singleOrNull()
                            (room?.get(Rooms.escapeDurationMinutes) ?: 5) * 60L
                        }

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
                                escapeDurationSeconds = escapeDurationSeconds,
                                totalDurationSeconds = totalDuration,
                                roleAssignments = roleAssignments
                            )
                        )

                        // Dynamic phase transition based on escape duration
                        val phaseTimerJob = CoroutineScope(Dispatchers.IO).launch {
                            val escapeDurationMillis = escapeDurationSeconds * 1000L
                            delay(escapeDurationMillis)

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
                                GameConnectionManager.updateGamePhase(gameId, "CHASE")
                                println("🏁 [GameTimer] Game $gameId: Phase changed to CHASE")
                            }
                        }
                        GameConnectionManager.registerPhaseTimer(gameId, phaseTimerJob)

                        // 전체 게임 타이머 시작 (도둑 승리 조건)
                        val timerJob = CoroutineScope(Dispatchers.IO).launch {
                            val totalDurationMillis = totalDuration * 1000L
                            println("⏰ [GameTimer] Game $gameId started: will end in ${totalDuration}s")
                            delay(totalDurationMillis)

                            // 시간 만료 시 게임 상태 확인
                            val gameStillActive = transaction {
                                Games.selectAll()
                                    .where { (Games.id eq gameId) and (Games.status eq "IN_PROGRESS") }
                                    .singleOrNull()
                                    ?.get(Games.status) == "IN_PROGRESS"
                            }

                            if (gameStillActive) {
                                println("⏰ [GameTimer] Time expired for game $gameId - THIEF wins")

                                // 도둑 승리 처리
                                transaction {
                                    Games.update({ Games.id eq gameId }) {
                                        it[status] = "FINISHED"
                                        it[winner] = "THIEF"
                                        it[endedAt] = Clock.System.now()
                                    }
                                }

                                // 모든 플레이어에게 게임 종료 브로드캐스트
                                GameConnectionManager.broadcastMessage(
                                    gameId,
                                    WebSocketMessage.GameEnded(com.heisthunt.shared.models.GameWinner.THIEF)
                                )

                                // 위치 데이터 정리 (DB 무한 증가 방지)
                                transaction { LocationUpdates.deleteWhere { LocationUpdates.gameId eq gameId } }
                                println("🏁 [GameTimer] Game $gameId ended: Time expired, THIEF wins")
                                GameConnectionManager.cancelGameTimer(gameId)
                            } else {
                                println("⏰ [GameTimer] Game $gameId already ended, timer cancelled")
                            }
                        }

                        // 타이머 Job 등록 (게임 종료 시 취소하기 위해)
                        GameConnectionManager.registerGameTimer(gameId, timerJob)

                        // 게임 시작 시 ESCAPE 페이즈로 캐시 초기화
                        GameConnectionManager.updateGamePhase(gameId, "ESCAPE")

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

                val policeShouldWin = transaction {
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
                        return@transaction false
                    }

                    // Check if all remaining thieves are caught (police win condition)
                    val gameInProgress = Games.selectAll()
                        .where { (Games.id eq gameId) and (Games.status eq "IN_PROGRESS") }
                        .singleOrNull() != null

                    if (gameInProgress) {
                        val uncaughtThieves = GamePlayers.selectAll()
                            .where {
                                (GamePlayers.gameId eq gameId) and
                                (GamePlayers.role eq "THIEF") and
                                (GamePlayers.isCaught eq false)
                            }
                            .count()
                        println("   Uncaught thieves remaining: $uncaughtThieves")
                        uncaughtThieves == 0L
                    } else false
                }

                if (policeShouldWin) {
                    transaction {
                        Games.update({ Games.id eq gameId }) {
                            it[status] = "FINISHED"
                            it[winner] = "POLICE"
                            it[endedAt] = Clock.System.now()
                        }
                    }
                    GameConnectionManager.broadcastMessage(
                        gameId,
                        WebSocketMessage.GameEnded(com.heisthunt.shared.models.GameWinner.POLICE)
                    )
                    transaction { LocationUpdates.deleteWhere { LocationUpdates.gameId eq gameId } }
                    GameConnectionManager.cancelGameTimer(gameId)
                    println("🏁 All thieves caught/left - POLICE wins (game $gameId)")
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

            // Get game result
            get("/{gameId}/result") {
                val gameId = call.parameters["gameId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, error = ErrorResponse(ErrorCodes.INTERNAL_ERROR, "Missing game ID"))
                )

                println("🏁 GET /games/$gameId/result")

                val result = transaction {
                    val game = Games.selectAll().where { Games.id eq gameId }.singleOrNull()
                        ?: return@transaction null

                    val players = (GamePlayers innerJoin Users)
                        .selectAll()
                        .where { GamePlayers.gameId eq gameId }
                        .map { row ->
                            com.heisthunt.shared.dto.PlayerResult(
                                userId = row[GamePlayers.userId],
                                nickname = row[Users.nickname],
                                role = PlayerRole.valueOf(row[GamePlayers.role]),
                                isCaught = row[GamePlayers.isCaught]
                            )
                        }

                    val duration = game[Games.endedAt]?.let { endedAt ->
                        (endedAt - game[Games.startedAt]).inWholeSeconds
                    } ?: 0L

                    com.heisthunt.shared.dto.GameResultResponse(
                        gameId = gameId,
                        winner = com.heisthunt.shared.models.GameWinner.valueOf(game[Games.winner] ?: "THIEF"),
                        duration = duration,
                        players = players
                    )
                }

                if (result != null) {
                    println("   ✅ Game result found: winner=${result.winner}, duration=${result.duration}s")
                    call.respond(ApiResponse(success = true, data = result))
                } else {
                    println("   ❌ Game not found")
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, error = ErrorResponse(ErrorCodes.GAME_NOT_FOUND, "Game not found"))
                    )
                }
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

                    // Calculate escape time remaining using actual room setting
                    val escapeTimeRemaining = if (game[Games.phase] == "ESCAPE") {
                        val escapeElapsed = (now - startedAt).inWholeSeconds
                        val escapeDurationSecs = room[Rooms.escapeDurationMinutes] * 60L
                        (escapeDurationSecs - escapeElapsed).coerceAtLeast(0)
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

            // Verify JWT signature and extract userId
            val userId = try {
                val verifier = JWT.require(Algorithm.HMAC256(jwtConfig.secret))
                    .withAudience(jwtConfig.audience)
                    .withIssuer(jwtConfig.issuer)
                    .build()
                val decodedJWT = verifier.verify(token)
                decodedJWT.getClaim("userId").asString() ?: run {
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token: missing userId claim"))
                    return@webSocket
                }
            } catch (e: Exception) {
                println("❌ Invalid or tampered JWT token: ${e.message}")
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
                                    // Save to database and check violations
                                    val (currentPhase, playerData) = transaction {
                                        LocationUpdates.insert {
                                            it[id] = UUID.randomUUID().toString()
                                            it[LocationUpdates.gameId] = gameId
                                            it[LocationUpdates.userId] = userId
                                            it[latitude] = message.location.latitude
                                            it[longitude] = message.location.longitude
                                            it[accuracy] = message.location.accuracy
                                            it[timestamp] = message.location.timestamp
                                        }

                                        // Get current game phase
                                        val phase = Games.selectAll()
                                            .where { Games.id eq gameId }
                                            .singleOrNull()
                                            ?.get(Games.phase) ?: "ESCAPE"

                                        // Get player data
                                        val player = GamePlayers.selectAll()
                                            .where { (GamePlayers.gameId eq gameId) and (GamePlayers.userId eq userId) }
                                            .singleOrNull()

                                        // 처음 위치 업데이트 시 감옥 위치 저장 (도둑의 경우)
                                        if (player != null && player[GamePlayers.role] == "THIEF" &&
                                            player[GamePlayers.jailLatitude] == null) {
                                            GamePlayers.update({
                                                (GamePlayers.gameId eq gameId) and (GamePlayers.userId eq userId)
                                            }) {
                                                it[jailLatitude] = message.location.latitude
                                                it[jailLongitude] = message.location.longitude
                                            }
                                            println("✅ Jail location saved for thief $userId: ${message.location.latitude}, ${message.location.longitude}")
                                        }

                                        // 경찰 감옥 위치 저장/갱신
                                        // GPS는 시작 후 30~60초간 정착하므로, 60초 동안 계속 갱신하여 정확한 위치 확보
                                        if (player != null && player[GamePlayers.role] == "POLICE") {
                                            val gameStartTime = Games.selectAll()
                                                .where { Games.id eq gameId }
                                                .singleOrNull()?.get(Games.startedAt)
                                            val elapsedSeconds = gameStartTime?.let {
                                                (Clock.System.now() - it).inWholeSeconds
                                            } ?: 999L

                                            val existingJailLat = player[GamePlayers.jailLatitude]
                                            if (existingJailLat == null || elapsedSeconds < 60) {
                                                GamePlayers.update({
                                                    (GamePlayers.gameId eq gameId) and (GamePlayers.userId eq userId)
                                                }) {
                                                    it[jailLatitude] = message.location.latitude
                                                    it[jailLongitude] = message.location.longitude
                                                }
                                                println("✅ Jail location updated for police $userId (elapsed: ${elapsedSeconds}s): ${message.location.latitude}, ${message.location.longitude}")
                                            }
                                        }

                                        Pair(phase, player)
                                    }

                                    // 경찰 감옥 위치를 게임 중심점으로 캐시
                                    if (playerData != null && playerData[GamePlayers.role] == "POLICE") {
                                        val jailLat = playerData[GamePlayers.jailLatitude]
                                        val jailLon = playerData[GamePlayers.jailLongitude]
                                        if (jailLat != null && jailLon != null) {
                                            GameConnectionManager.updateGameCenter(gameId, jailLat, jailLon)
                                        }
                                    }

                                    // ESCAPE 페이즈에서 경찰 이동 제한 확인
                                    if (playerData != null && playerData[GamePlayers.role] == "POLICE" && currentPhase == "ESCAPE") {
                                        val jailLat = playerData[GamePlayers.jailLatitude]
                                        val jailLon = playerData[GamePlayers.jailLongitude]

                                        if (jailLat != null && jailLon != null) {
                                            val distance = GeoUtils.calculateDistance(
                                                jailLat, jailLon,
                                                message.location.latitude, message.location.longitude
                                            )

                                            // GPS accuracy를 고려한 이탈 판정
                                            // 실내에서는 GPS 오차가 10~30m이므로 accuracy보다 멀어야 실제 이탈로 판정
                                            // 최소 threshold는 15m (실내 GPS 오차 기본값)
                                            val gpsAccuracy = message.location.accuracy?.toDouble() ?: 15.0
                                            val threshold = maxOf(15.0, gpsAccuracy)
                                            if (distance > threshold) {
                                                println("⚠️ Police $userId violated jail (${distance}m from jail, gpsAccuracy=${gpsAccuracy}m, threshold=${threshold}m)")
                                                GameConnectionManager.broadcastToRole(
                                                    gameId,
                                                    PlayerRole.THIEF,
                                                    WebSocketMessage.PoliceViolation(
                                                        policeUserId = userId,
                                                        distanceFromJail = distance
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    // 도둑 게임 반경 이탈 감지 (모든 페이즈)
                                    if (playerData != null && playerData[GamePlayers.role] == "THIEF" &&
                                        !playerData[GamePlayers.isCaught]) {
                                        val gameCenter = GameConnectionManager.getGameCenter(gameId)
                                        if (gameCenter != null) {
                                            val distance = GeoUtils.calculateDistance(
                                                gameCenter.first, gameCenter.second,
                                                message.location.latitude, message.location.longitude
                                            )
                                            val isOutside = distance > 500.0
                                            val result = GameConnectionManager.checkThiefBoundary(gameId, userId, isOutside)
                                            if (result != null) {
                                                val (warningCount, isSentToJail) = result
                                                val thiefNickname = transaction {
                                                    (GamePlayers innerJoin Users)
                                                        .selectAll()
                                                        .where { (GamePlayers.gameId eq gameId) and (GamePlayers.userId eq userId) }
                                                        .singleOrNull()?.get(Users.nickname) ?: userId
                                                }
                                                val violationMsg = WebSocketMessage.ThiefBoundaryViolation(
                                                    thiefUserId = userId,
                                                    thiefNickname = thiefNickname,
                                                    warningCount = warningCount,
                                                    isSentToJail = isSentToJail
                                                )
                                                println("⚠️ [Boundary] Thief $userId ($thiefNickname) ${distance}m from center, warning=$warningCount, jail=$isSentToJail")
                                                GameConnectionManager.broadcastToRole(gameId, PlayerRole.POLICE, violationMsg)
                                                GameConnectionManager.sendToUser(gameId, userId, violationMsg)

                                                if (isSentToJail) {
                                                    transaction {
                                                        GamePlayers.update({
                                                            (GamePlayers.gameId eq gameId) and (GamePlayers.userId eq userId)
                                                        }) { it[isCaught] = true }
                                                    }
                                                    GameConnectionManager.markPlayerCaught(gameId, userId)
                                                    GameConnectionManager.broadcastMessage(gameId, WebSocketMessage.PlayerCaught(userId, thiefNickname))
                                                    val uncaughtThieves = transaction {
                                                        GamePlayers.selectAll().where {
                                                            (GamePlayers.gameId eq gameId) and
                                                            (GamePlayers.role eq "THIEF") and
                                                            (GamePlayers.isCaught eq false)
                                                        }.count()
                                                    }
                                                    if (uncaughtThieves == 0L) {
                                                        transaction {
                                                            Games.update({ Games.id eq gameId }) {
                                                                it[status] = "FINISHED"
                                                                it[winner] = "POLICE"
                                                                it[endedAt] = Clock.System.now()
                                                            }
                                                        }
                                                        GameConnectionManager.broadcastMessage(gameId, WebSocketMessage.GameEnded(com.heisthunt.shared.models.GameWinner.POLICE))
                                                        transaction { LocationUpdates.deleteWhere { LocationUpdates.gameId eq gameId } }
                                                        GameConnectionManager.cancelGameTimer(gameId)
                                                        println("🏁 All thieves caught (boundary) - POLICE wins (game $gameId)")
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Update cache and broadcast
                                    GameConnectionManager.updateLocation(gameId, userId, message.location)
                                    GameConnectionManager.broadcastLocations(gameId, userId)

                                    // CHASE 페이즈: 근접 적 감지 후 개별 알림 전송
                                    if (currentPhase == "CHASE") {
                                        GameConnectionManager.sendProximityAlerts(gameId, userId)
                                    }
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

                                    // 체포된 도둑을 위치 브로드캐스트에서 즉시 제외
                                    GameConnectionManager.markPlayerCaught(gameId, userId)

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
                                        // 위치 데이터 정리
                                        transaction { LocationUpdates.deleteWhere { LocationUpdates.gameId eq gameId } }
                                        println("🏁 [GameTimer] Game $gameId ended - POLICE wins (all thieves caught)")

                                        // 타이머 취소 (경찰이 이겼으므로 도둑 승리 조건 불필요)
                                        GameConnectionManager.cancelGameTimer(gameId)
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

                                is WebSocketMessage.RTCOffer -> {
                                    // WebRTC offer relay: sender → target user
                                    println("📡 [RTC] RTCOffer relay: ${message.fromUserId} → ${message.toUserId}")
                                    GameConnectionManager.sendToUser(gameId, message.toUserId, message)
                                }

                                is WebSocketMessage.RTCAnswer -> {
                                    // WebRTC answer relay: sender → target user
                                    println("📡 [RTC] RTCAnswer relay: ${message.fromUserId} → ${message.toUserId}")
                                    GameConnectionManager.sendToUser(gameId, message.toUserId, message)
                                }

                                is WebSocketMessage.RTCIceCandidate -> {
                                    // WebRTC ICE candidate relay: sender → target user
                                    println("🧊 [RTC] RTCIceCandidate relay: ${message.fromUserId} → ${message.toUserId}")
                                    GameConnectionManager.sendToUser(gameId, message.toUserId, message)
                                }

                                is WebSocketMessage.RTCPeerLeft -> {
                                    // WebRTC peer left: broadcast to same role teammates
                                    println("🔌 [RTC] RTCPeerLeft: $userId left voice channel")
                                    val senderRole = transaction {
                                        GamePlayers.selectAll().where {
                                            (GamePlayers.gameId eq gameId) and
                                            (GamePlayers.userId eq userId)
                                        }.singleOrNull()?.let {
                                            PlayerRole.valueOf(it[GamePlayers.role])
                                        }
                                    }
                                    if (senderRole != null) {
                                        GameConnectionManager.broadcastToRole(gameId, senderRole, message)
                                    } else {
                                        GameConnectionManager.broadcastMessage(gameId, message)
                                    }
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

private sealed class ActiveGameResult {
    data class Found(val response: com.heisthunt.shared.dto.ActiveGameResponse) : ActiveGameResult()
    data class Expired(val gameId: String) : ActiveGameResult()
    data object NotFound : ActiveGameResult()
}
