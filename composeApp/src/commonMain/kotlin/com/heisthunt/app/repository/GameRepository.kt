package com.heisthunt.app.repository

import com.heisthunt.shared.dto.ApiResponse
import com.heisthunt.shared.dto.CatchRequest
import com.heisthunt.shared.dto.CatchResponse
import com.heisthunt.shared.dto.GameStateResponse
import com.heisthunt.app.network.ApiClient

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
}
