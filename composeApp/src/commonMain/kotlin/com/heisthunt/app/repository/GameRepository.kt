package com.heisthunt.app.repository

import com.heisthunt.shared.dto.ActiveGameResponse
import com.heisthunt.shared.dto.ApiResponse
import com.heisthunt.shared.dto.CatchRequest
import com.heisthunt.shared.dto.CatchResponse
import com.heisthunt.shared.dto.GameStateResponse
import com.heisthunt.app.network.ApiClient
import io.ktor.http.*

class GameRepository(private val apiClient: ApiClient) {

    suspend fun getGameStatus(gameId: String): Result<GameStateResponse?> {
        return try {
            val response = apiClient.get<ApiResponse<GameStateResponse>>("/games/$gameId/status")
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to get game status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun catchPlayer(gameId: String, targetUserId: String): Result<CatchResponse?> {
        return try {
            val response = apiClient.post<CatchRequest, ApiResponse<CatchResponse>>(
                "/games/$gameId/catch",
                CatchRequest(targetUserId)
            )
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to catch player"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyActiveGame(): Result<ActiveGameResponse?> {
        return try {
            val response = apiClient.get<ApiResponse<ActiveGameResponse>>("/api/games/my-active-game")
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                // Return null if no active game (404 is expected)
                Result.success(null)
            }
        } catch (e: Exception) {
            // If 404, return null instead of failure
            if (e.message?.contains("404") == true) {
                Result.success(null)
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun leaveGame(gameId: String): Result<Unit> {
        return try {
            val response = apiClient.leaveGame(gameId)
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to leave game"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
