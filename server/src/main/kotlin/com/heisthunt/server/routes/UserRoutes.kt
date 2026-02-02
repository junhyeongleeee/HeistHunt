package com.heisthunt.server.routes

import com.heisthunt.server.database.Users
import com.heisthunt.shared.dto.ApiResponse
import com.heisthunt.shared.dto.ErrorCodes
import com.heisthunt.shared.dto.ErrorResponse
import com.heisthunt.shared.models.User
import com.heisthunt.shared.models.UserStats
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.userRoutes() {
    route("/users") {
        authenticate("auth-jwt") {
            get("/me") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()

                if (userId == null) {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        ApiResponse<Unit>(success = false, error = ErrorResponse(ErrorCodes.INVALID_TOKEN, "Invalid token"))
                    )
                    return@get
                }

                val user = transaction {
                    Users.selectAll().where { Users.id eq userId }.singleOrNull()?.let { row ->
                        User(
                            id = row[Users.id],
                            email = row[Users.email],
                            nickname = row[Users.nickname],
                            profileImageUrl = row[Users.profileImageUrl],
                            stats = UserStats(
                                totalGames = row[Users.totalGames],
                                wins = row[Users.wins],
                                losses = row[Users.losses],
                                policeWins = row[Users.policeWins],
                                thiefWins = row[Users.thiefWins]
                            ),
                            createdAt = row[Users.createdAt],
                            updatedAt = row[Users.updatedAt]
                        )
                    }
                }

                if (user != null) {
                    call.respond(ApiResponse(success = true, data = user))
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, error = ErrorResponse(ErrorCodes.USER_NOT_FOUND, "User not found"))
                    )
                }
            }

            get("/{id}") {
                val targetUserId = call.parameters["id"]

                if (targetUserId == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, error = ErrorResponse("BAD_REQUEST", "User ID required"))
                    )
                    return@get
                }

                val user = transaction {
                    Users.selectAll().where { Users.id eq targetUserId }.singleOrNull()?.let { row ->
                        User(
                            id = row[Users.id],
                            email = "", // Hide email for privacy
                            nickname = row[Users.nickname],
                            profileImageUrl = row[Users.profileImageUrl],
                            stats = UserStats(
                                totalGames = row[Users.totalGames],
                                wins = row[Users.wins],
                                losses = row[Users.losses],
                                policeWins = row[Users.policeWins],
                                thiefWins = row[Users.thiefWins]
                            ),
                            createdAt = row[Users.createdAt],
                            updatedAt = row[Users.updatedAt]
                        )
                    }
                }

                if (user != null) {
                    call.respond(ApiResponse(success = true, data = user))
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, error = ErrorResponse(ErrorCodes.USER_NOT_FOUND, "User not found"))
                    )
                }
            }
        }
    }
}
