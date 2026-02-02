package com.heisthunt.server.plugins

import com.heisthunt.shared.dto.ApiResponse
import com.heisthunt.shared.dto.ErrorCodes
import com.heisthunt.shared.dto.ErrorResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiResponse<Unit>(
                    success = false,
                    error = ErrorResponse(
                        code = ErrorCodes.INTERNAL_ERROR,
                        message = cause.message ?: "Internal server error"
                    )
                )
            )
        }

        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(
                    success = false,
                    error = ErrorResponse(
                        code = "BAD_REQUEST",
                        message = cause.message ?: "Invalid request"
                    )
                )
            )
        }

        status(HttpStatusCode.Unauthorized) { call, _ ->
            call.respond(
                HttpStatusCode.Unauthorized,
                ApiResponse<Unit>(
                    success = false,
                    error = ErrorResponse(
                        code = ErrorCodes.INVALID_TOKEN,
                        message = "Authentication required"
                    )
                )
            )
        }

        status(HttpStatusCode.NotFound) { call, _ ->
            call.respond(
                HttpStatusCode.NotFound,
                ApiResponse<Unit>(
                    success = false,
                    error = ErrorResponse(
                        code = "NOT_FOUND",
                        message = "Resource not found"
                    )
                )
            )
        }
    }
}
