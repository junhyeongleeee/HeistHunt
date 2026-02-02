package com.heisthunt.shared.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorResponse? = null
)

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String
)

object ErrorCodes {
    const val INVALID_CREDENTIALS = "INVALID_CREDENTIALS"
    const val USER_NOT_FOUND = "USER_NOT_FOUND"
    const val EMAIL_ALREADY_EXISTS = "EMAIL_ALREADY_EXISTS"
    const val NICKNAME_ALREADY_EXISTS = "NICKNAME_ALREADY_EXISTS"
    const val INVALID_TOKEN = "INVALID_TOKEN"
    const val TOKEN_EXPIRED = "TOKEN_EXPIRED"
    const val ROOM_NOT_FOUND = "ROOM_NOT_FOUND"
    const val ROOM_FULL = "ROOM_FULL"
    const val INVALID_PASSWORD = "INVALID_PASSWORD"
    const val NOT_HOST = "NOT_HOST"
    const val GAME_NOT_FOUND = "GAME_NOT_FOUND"
    const val GAME_NOT_IN_PROGRESS = "GAME_NOT_IN_PROGRESS"
    const val INVALID_CATCH = "INVALID_CATCH"
    const val INTERNAL_ERROR = "INTERNAL_ERROR"
}
