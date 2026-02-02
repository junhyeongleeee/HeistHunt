package com.heisthunt.server.routes

import com.heisthunt.server.database.RoomParticipants
import com.heisthunt.server.database.Rooms
import com.heisthunt.server.database.Users
import com.heisthunt.server.websocket.RoomConnectionManager
import com.heisthunt.shared.dto.*
import com.heisthunt.shared.models.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

fun Route.roomRoutes() {
    route("/rooms") {
        authenticate("auth-jwt") {
            // Create room
            post {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: return@post

                val request = call.receive<CreateRoomRequest>()
                val now = Clock.System.now()
                val roomId = UUID.randomUUID().toString()
                val roomCode = generateRoomCode()

                val room = transaction {
                    Rooms.insert {
                        it[id] = roomId
                        it[code] = roomCode
                        it[name] = request.name
                        it[hostId] = userId
                        it[maxPlayers] = request.settings.maxPlayers
                        it[policeRatio] = request.settings.policeRatio
                        it[gameDurationMinutes] = request.settings.gameDurationMinutes
                        it[password] = request.settings.password
                        it[status] = RoomStatus.WAITING.name
                        it[createdAt] = now
                        it[updatedAt] = now
                    }

                    // Add host as participant
                    RoomParticipants.insert {
                        it[id] = UUID.randomUUID().toString()
                        it[RoomParticipants.roomId] = roomId
                        it[RoomParticipants.userId] = userId
                        it[isReady] = false // 방장도 역할 선택 후 준비 필요
                        it[joinedAt] = now
                    }

                    getRoomById(roomId)
                }

                if (room != null) {
                    call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = room))
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<Unit>(success = false, error = ErrorResponse(ErrorCodes.INTERNAL_ERROR, "Failed to create room"))
                    )
                }
            }

            // List rooms
            get {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20

                val result = transaction {
                    val totalCount = Rooms.selectAll()
                        .where { Rooms.status eq RoomStatus.WAITING.name }
                        .count().toInt()

                    val rooms = Rooms.selectAll()
                        .where { Rooms.status eq RoomStatus.WAITING.name }
                        .orderBy(Rooms.createdAt, SortOrder.DESC)
                        .limit(pageSize).offset(((page - 1) * pageSize).toLong())
                        .map { row ->
                            val participantCount = RoomParticipants.selectAll()
                                .where { RoomParticipants.roomId eq row[Rooms.id] }
                                .count().toInt()

                            val hostNickname = Users.selectAll()
                                .where { Users.id eq row[Rooms.hostId] }
                                .singleOrNull()?.get(Users.nickname) ?: "Unknown"

                            RoomSummary(
                                id = row[Rooms.id],
                                code = row[Rooms.code],
                                name = row[Rooms.name],
                                hostNickname = hostNickname,
                                currentPlayers = participantCount,
                                maxPlayers = row[Rooms.maxPlayers],
                                hasPassword = row[Rooms.password] != null,
                                status = row[Rooms.status]
                            )
                        }

                    RoomListResponse(rooms, totalCount, page, pageSize)
                }

                call.respond(ApiResponse(success = true, data = result))
            }

            // Get room by ID
            get("/{id}") {
                val roomId = call.parameters["id"] ?: return@get

                val room = transaction { getRoomById(roomId) }

                if (room != null) {
                    call.respond(ApiResponse(success = true, data = room))
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, error = ErrorResponse(ErrorCodes.ROOM_NOT_FOUND, "Room not found"))
                    )
                }
            }

            // Join room
            post("/join") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: return@post

                val request = call.receive<JoinRoomRequest>()
                val now = Clock.System.now()

                data class JoinData(val result: JoinResult, val roomId: String?, val newParticipant: Participant?)

                val joinData = transaction {
                    val roomRow = Rooms.selectAll()
                        .where { Rooms.code eq request.code }
                        .singleOrNull() ?: return@transaction JoinData(JoinResult.RoomNotFound, null, null)

                    val roomId = roomRow[Rooms.id]

                    // Check password
                    val roomPassword = roomRow[Rooms.password]
                    if (roomPassword != null && roomPassword != request.password) {
                        return@transaction JoinData(JoinResult.InvalidPassword, null, null)
                    }

                    // Check if room is full
                    val currentCount = RoomParticipants.selectAll()
                        .where { RoomParticipants.roomId eq roomId }
                        .count().toInt()

                    if (currentCount >= roomRow[Rooms.maxPlayers]) {
                        return@transaction JoinData(JoinResult.RoomFull, null, null)
                    }

                    // Check if already joined
                    val existing = RoomParticipants.selectAll()
                        .where { (RoomParticipants.roomId eq roomId) and (RoomParticipants.userId eq userId) }
                        .singleOrNull()

                    var newParticipant: Participant? = null
                    if (existing == null) {
                        RoomParticipants.insert {
                            it[id] = UUID.randomUUID().toString()
                            it[RoomParticipants.roomId] = roomId
                            it[RoomParticipants.userId] = userId
                            it[isReady] = false
                            it[joinedAt] = now
                        }

                        // Get participant info for broadcasting
                        val userRow = Users.selectAll().where { Users.id eq userId }.singleOrNull()
                        if (userRow != null) {
                            newParticipant = Participant(
                                userId = userId,
                                nickname = userRow[Users.nickname],
                                isReady = false,
                                selectedRole = null,
                                role = null,
                                joinedAt = now
                            )
                        }
                    }

                    JoinData(JoinResult.Success(getRoomById(roomId)!!), roomId, newParticipant)
                }

                when (val result = joinData.result) {
                    is JoinResult.Success -> {
                        call.respond(ApiResponse(success = true, data = result.room))

                        // Broadcast participant joined event
                        if (joinData.newParticipant != null && joinData.roomId != null) {
                            RoomConnectionManager.broadcast(
                                joinData.roomId,
                                RoomEvent.ParticipantJoined(joinData.newParticipant)
                            )
                        }
                    }
                    JoinResult.RoomNotFound -> call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, error = ErrorResponse(ErrorCodes.ROOM_NOT_FOUND, "Room not found"))
                    )
                    JoinResult.RoomFull -> call.respond(
                        HttpStatusCode.Conflict,
                        ApiResponse<Unit>(success = false, error = ErrorResponse(ErrorCodes.ROOM_FULL, "Room is full"))
                    )
                    JoinResult.InvalidPassword -> call.respond(
                        HttpStatusCode.Unauthorized,
                        ApiResponse<Unit>(success = false, error = ErrorResponse(ErrorCodes.INVALID_PASSWORD, "Invalid password"))
                    )
                }
            }

            // Leave room
            post("/{id}/leave") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: return@post
                val roomId = call.parameters["id"] ?: return@post

                transaction {
                    RoomParticipants.deleteWhere {
                        (RoomParticipants.roomId eq roomId) and (RoomParticipants.userId eq userId)
                    }

                    // If no participants left, delete room
                    val remainingCount = RoomParticipants.selectAll()
                        .where { RoomParticipants.roomId eq roomId }
                        .count()

                    if (remainingCount == 0L) {
                        Rooms.deleteWhere { Rooms.id eq roomId }
                    }
                }

                call.respond(ApiResponse(success = true, data = "Left room"))

                // Broadcast participant left event
                RoomConnectionManager.broadcast(
                    roomId,
                    RoomEvent.ParticipantLeft(userId)
                )
            }

            // Select role
            post("/{id}/select-role") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: return@post
                val roomId = call.parameters["id"] ?: return@post
                val request = call.receive<SelectRoleRequest>()

                transaction {
                    // Check if participant exists
                    val participant = RoomParticipants.selectAll()
                        .where { (RoomParticipants.roomId eq roomId) and (RoomParticipants.userId eq userId) }
                        .singleOrNull()

                    if (participant != null) {
                        RoomParticipants.update({
                            (RoomParticipants.roomId eq roomId) and (RoomParticipants.userId eq userId)
                        }) {
                            it[selectedRole] = request.role.name
                        }
                    }
                }

                val room = transaction { getRoomById(roomId) }
                call.respond(ApiResponse(success = true, data = room))

                // Broadcast role selection
                RoomConnectionManager.broadcast(
                    roomId,
                    RoomEvent.RoleSelected(userId, request.role)
                )
            }

            // Toggle ready status
            post("/{id}/ready") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: return@post
                val roomId = call.parameters["id"] ?: return@post

                val newReady = transaction {
                    val current = RoomParticipants.selectAll()
                        .where { (RoomParticipants.roomId eq roomId) and (RoomParticipants.userId eq userId) }
                        .singleOrNull()

                    if (current != null) {
                        val newReadyValue = !current[RoomParticipants.isReady]
                        RoomParticipants.update({
                            (RoomParticipants.roomId eq roomId) and (RoomParticipants.userId eq userId)
                        }) {
                            it[isReady] = newReadyValue
                        }
                        newReadyValue
                    } else {
                        null
                    }
                }

                val room = transaction { getRoomById(roomId) }
                call.respond(ApiResponse(success = true, data = room))

                // Broadcast ready status change
                if (newReady != null) {
                    RoomConnectionManager.broadcast(
                        roomId,
                        RoomEvent.ParticipantReady(userId, newReady)
                    )
                }
            }
        }
    }
}

private fun getRoomById(roomId: String): Room? {
    val roomRow = Rooms.selectAll().where { Rooms.id eq roomId }.singleOrNull() ?: return null

    val participants = (RoomParticipants innerJoin Users)
        .selectAll()
        .where { RoomParticipants.roomId eq roomId }
        .map { row ->
            Participant(
                userId = row[RoomParticipants.userId],
                nickname = row[Users.nickname],
                isReady = row[RoomParticipants.isReady],
                selectedRole = row[RoomParticipants.selectedRole]?.let { PlayerRole.valueOf(it) },
                role = row[RoomParticipants.role]?.let { PlayerRole.valueOf(it) },
                joinedAt = row[RoomParticipants.joinedAt]
            )
        }

    return Room(
        id = roomRow[Rooms.id],
        code = roomRow[Rooms.code],
        name = roomRow[Rooms.name],
        hostId = roomRow[Rooms.hostId],
        settings = RoomSettings(
            maxPlayers = roomRow[Rooms.maxPlayers],
            policeRatio = roomRow[Rooms.policeRatio],
            gameDurationMinutes = roomRow[Rooms.gameDurationMinutes],
            password = roomRow[Rooms.password]
        ),
        status = RoomStatus.valueOf(roomRow[Rooms.status]),
        participants = participants,
        createdAt = roomRow[Rooms.createdAt],
        updatedAt = roomRow[Rooms.updatedAt]
    )
}

private fun generateRoomCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return (1..6).map { chars.random() }.joinToString("")
}

private sealed class JoinResult {
    data class Success(val room: Room) : JoinResult()
    data object RoomNotFound : JoinResult()
    data object RoomFull : JoinResult()
    data object InvalidPassword : JoinResult()
}
