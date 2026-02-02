package com.heisthunt.server.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.heisthunt.server.plugins.JwtConfig
import com.heisthunt.server.websocket.RoomConnectionManager
import com.heisthunt.shared.dto.RoomAction
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json

fun Route.roomWebSocketRoutes(jwtConfig: JwtConfig) {
    webSocket("/rooms/{roomId}/ws") {
        val roomId = call.parameters["roomId"] ?: run {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Room ID required"))
            return@webSocket
        }

        // Get token from query parameter
        val token = call.request.queryParameters["token"] ?: run {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Token required"))
            return@webSocket
        }

        // Verify token manually
        val userId = try {
            val verifier = JWT.require(Algorithm.HMAC256(jwtConfig.secret))
                .withAudience(jwtConfig.audience)
                .withIssuer(jwtConfig.issuer)
                .build()
            val decodedJWT = verifier.verify(token)
            decodedJWT.getClaim("userId").asString() ?: run {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
                return@webSocket
            }
        } catch (e: Exception) {
            println("Token verification failed: ${e.message}")
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
            return@webSocket
        }

        println("WebSocket connection established: userId=$userId, roomId=$roomId")

        // Subscribe to room updates
        RoomConnectionManager.subscribe(roomId, userId, this)

        try {
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        println("Received WebSocket message from user $userId: $text")

                        try {
                            val action = Json.decodeFromString<RoomAction>(text)
                            println("Parsed action: ${action::class.simpleName}")
                            // Actions will be handled in future iterations
                            // For now, room state changes come from REST endpoints
                        } catch (e: Exception) {
                            println("Error parsing action: ${e.message}")
                        }
                    }
                    else -> {
                        println("Received non-text frame: ${frame.frameType}")
                    }
                }
            }
        } catch (e: Exception) {
            println("WebSocket error for user $userId: ${e.message}")
        } finally {
            RoomConnectionManager.unsubscribe(roomId, userId, this)
            println("WebSocket connection closed: userId=$userId, roomId=$roomId")
        }
    }
}
