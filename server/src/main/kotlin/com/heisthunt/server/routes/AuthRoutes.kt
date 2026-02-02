package com.heisthunt.server.routes

import com.heisthunt.server.auth.JwtService
import com.heisthunt.server.auth.PasswordHasher
import com.heisthunt.server.database.RefreshTokens
import com.heisthunt.server.database.Users
import com.heisthunt.server.plugins.JwtConfig
import com.heisthunt.shared.dto.*
import com.heisthunt.shared.models.User
import com.heisthunt.shared.models.UserStats
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

fun Route.authRoutes() {
    val jwtConfig = JwtConfig.fromEnvironment(application.environment.config)
    val jwtService = JwtService(jwtConfig)

    route("/auth") {
        post("/register") {
            val request = call.receive<RegisterRequest>()

            // Validate input
            require(request.email.isNotBlank()) { "Email is required" }
            require(request.password.length >= 6) { "Password must be at least 6 characters" }
            require(request.nickname.length in 2..20) { "Nickname must be 2-20 characters" }

            val result = transaction {
                // Check if email exists
                val existingEmail = Users.selectAll().where { Users.email eq request.email }.singleOrNull()
                if (existingEmail != null) {
                    return@transaction RegisterResult.EmailExists
                }

                // Check if nickname exists
                val existingNickname = Users.selectAll().where { Users.nickname eq request.nickname }.singleOrNull()
                if (existingNickname != null) {
                    return@transaction RegisterResult.NicknameExists
                }

                val userId = UUID.randomUUID().toString()
                val now = Clock.System.now()
                val passwordHash = PasswordHasher.hash(request.password)

                Users.insert {
                    it[id] = userId
                    it[email] = request.email
                    it[Users.passwordHash] = passwordHash
                    it[nickname] = request.nickname
                    it[createdAt] = now
                    it[updatedAt] = now
                }

                val accessToken = jwtService.generateAccessToken(userId, request.email)
                val refreshToken = jwtService.generateRefreshToken(userId)

                // Save refresh token
                RefreshTokens.insert {
                    it[id] = UUID.randomUUID().toString()
                    it[RefreshTokens.userId] = userId
                    it[token] = refreshToken
                    it[expiresAt] = jwtService.getRefreshTokenExpiresAt()
                    it[createdAt] = now
                }

                val user = User(
                    id = userId,
                    email = request.email,
                    nickname = request.nickname,
                    stats = UserStats(),
                    createdAt = now,
                    updatedAt = now
                )

                RegisterResult.Success(AuthResponse(accessToken, refreshToken, user))
            }

            when (result) {
                is RegisterResult.Success -> {
                    call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = result.response))
                }
                RegisterResult.EmailExists -> {
                    call.respond(
                        HttpStatusCode.Conflict,
                        ApiResponse<Unit>(success = false, error = ErrorResponse(ErrorCodes.EMAIL_ALREADY_EXISTS, "Email already exists"))
                    )
                }
                RegisterResult.NicknameExists -> {
                    call.respond(
                        HttpStatusCode.Conflict,
                        ApiResponse<Unit>(success = false, error = ErrorResponse(ErrorCodes.NICKNAME_ALREADY_EXISTS, "Nickname already exists"))
                    )
                }
            }
        }

        post("/login") {
            val request = call.receive<LoginRequest>()

            val result = transaction {
                val userRow = Users.selectAll().where { Users.email eq request.email }.singleOrNull()
                    ?: return@transaction LoginResult.InvalidCredentials

                val passwordValid = PasswordHasher.verify(request.password, userRow[Users.passwordHash])
                if (!passwordValid) {
                    return@transaction LoginResult.InvalidCredentials
                }

                val userId = userRow[Users.id]
                val accessToken = jwtService.generateAccessToken(userId, request.email)
                val refreshToken = jwtService.generateRefreshToken(userId)

                // Save refresh token
                val now = Clock.System.now()
                RefreshTokens.insert {
                    it[id] = UUID.randomUUID().toString()
                    it[RefreshTokens.userId] = userId
                    it[token] = refreshToken
                    it[expiresAt] = jwtService.getRefreshTokenExpiresAt()
                    it[createdAt] = now
                }

                val user = User(
                    id = userId,
                    email = userRow[Users.email],
                    nickname = userRow[Users.nickname],
                    profileImageUrl = userRow[Users.profileImageUrl],
                    stats = UserStats(
                        totalGames = userRow[Users.totalGames],
                        wins = userRow[Users.wins],
                        losses = userRow[Users.losses],
                        policeWins = userRow[Users.policeWins],
                        thiefWins = userRow[Users.thiefWins]
                    ),
                    createdAt = userRow[Users.createdAt],
                    updatedAt = userRow[Users.updatedAt]
                )

                LoginResult.Success(AuthResponse(accessToken, refreshToken, user))
            }

            when (result) {
                is LoginResult.Success -> {
                    call.respond(ApiResponse(success = true, data = result.response))
                }
                LoginResult.InvalidCredentials -> {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, error = ErrorResponse(ErrorCodes.INVALID_CREDENTIALS, "Invalid email or password"))
                    )
                }
            }
        }

        post("/google") {
            val request = call.receive<GoogleLoginRequest>()

            // For now, we'll trust the client and create/login user based on Google email
            // TODO: Add Firebase Admin SDK verification in production

            // Extract email from ID token (simplified - in production, verify token first)
            val email = request.idToken // Temporarily using idToken as email

            val result = transaction {
                // Check if user exists
                val existingUser = Users.selectAll().where { Users.email eq email }.singleOrNull()

                val userId: String
                val user: User

                if (existingUser != null) {
                    // User exists, login
                    userId = existingUser[Users.id]
                    user = User(
                        id = userId,
                        email = existingUser[Users.email],
                        nickname = existingUser[Users.nickname],
                        profileImageUrl = existingUser[Users.profileImageUrl],
                        stats = UserStats(
                            totalGames = existingUser[Users.totalGames],
                            wins = existingUser[Users.wins],
                            losses = existingUser[Users.losses],
                            policeWins = existingUser[Users.policeWins],
                            thiefWins = existingUser[Users.thiefWins]
                        ),
                        createdAt = existingUser[Users.createdAt],
                        updatedAt = existingUser[Users.updatedAt]
                    )
                } else {
                    // Create new user
                    userId = UUID.randomUUID().toString()
                    val now = Clock.System.now()
                    val nickname = email.substringBefore("@")

                    Users.insert {
                        it[id] = userId
                        it[Users.email] = email
                        it[passwordHash] = "" // No password for Google login
                        it[Users.nickname] = nickname
                        it[createdAt] = now
                        it[updatedAt] = now
                    }

                    user = User(
                        id = userId,
                        email = email,
                        nickname = nickname,
                        stats = UserStats(),
                        createdAt = now,
                        updatedAt = now
                    )
                }

                val accessToken = jwtService.generateAccessToken(userId, email)
                val refreshToken = jwtService.generateRefreshToken(userId)

                // Save refresh token
                val now = Clock.System.now()
                RefreshTokens.insert {
                    it[id] = UUID.randomUUID().toString()
                    it[RefreshTokens.userId] = userId
                    it[token] = refreshToken
                    it[expiresAt] = jwtService.getRefreshTokenExpiresAt()
                    it[createdAt] = now
                }

                AuthResponse(accessToken, refreshToken, user)
            }

            call.respond(ApiResponse(success = true, data = result))
        }

        post("/refresh") {
            val request = call.receive<RefreshTokenRequest>()

            val userId = jwtService.verifyRefreshToken(request.refreshToken)
            if (userId == null) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Unit>(success = false, error = ErrorResponse(ErrorCodes.INVALID_TOKEN, "Invalid refresh token"))
                )
                return@post
            }

            val result = transaction {
                // Check if refresh token exists in DB
                val tokenRow = RefreshTokens.selectAll()
                    .where { RefreshTokens.token eq request.refreshToken }
                    .singleOrNull() ?: return@transaction null

                // Delete old refresh token
                RefreshTokens.deleteWhere { token eq request.refreshToken }

                // Get user
                val userRow = Users.selectAll().where { Users.id eq userId }.singleOrNull()
                    ?: return@transaction null

                // Generate new tokens
                val accessToken = jwtService.generateAccessToken(userId, userRow[Users.email])
                val newRefreshToken = jwtService.generateRefreshToken(userId)

                // Save new refresh token
                val now = Clock.System.now()
                RefreshTokens.insert {
                    it[id] = UUID.randomUUID().toString()
                    it[RefreshTokens.userId] = userId
                    it[token] = newRefreshToken
                    it[expiresAt] = jwtService.getRefreshTokenExpiresAt()
                    it[createdAt] = now
                }

                TokenResponse(accessToken, newRefreshToken)
            }

            if (result != null) {
                call.respond(ApiResponse(success = true, data = result))
            } else {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Unit>(success = false, error = ErrorResponse(ErrorCodes.INVALID_TOKEN, "Token not found"))
                )
            }
        }
    }
}

private sealed class RegisterResult {
    data class Success(val response: AuthResponse) : RegisterResult()
    data object EmailExists : RegisterResult()
    data object NicknameExists : RegisterResult()
}

private sealed class LoginResult {
    data class Success(val response: AuthResponse) : LoginResult()
    data object InvalidCredentials : LoginResult()
}
