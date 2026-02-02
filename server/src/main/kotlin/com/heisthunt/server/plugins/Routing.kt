package com.heisthunt.server.plugins

import com.heisthunt.server.routes.authRoutes
import com.heisthunt.server.routes.gameRoutes
import com.heisthunt.server.routes.roomRoutes
import com.heisthunt.server.routes.roomWebSocketRoutes
import com.heisthunt.server.routes.userRoutes
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val config = environment.config
    val jwtConfig = JwtConfig.fromEnvironment(config)

    routing {
        get("/") {
            call.respondText("HeistHunt API Server")
        }

        get("/health") {
            call.respondText("OK")
        }

        route("/api") {
            authRoutes()
            userRoutes()
            roomRoutes()
            roomWebSocketRoutes(jwtConfig)
            gameRoutes()
        }
    }
}
