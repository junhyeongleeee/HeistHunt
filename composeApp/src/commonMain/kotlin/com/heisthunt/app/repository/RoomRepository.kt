package com.heisthunt.app.repository

import com.heisthunt.app.network.ApiClient
import com.heisthunt.shared.dto.CreateRoomRequest
import com.heisthunt.shared.dto.JoinRoomRequest
import com.heisthunt.shared.dto.RoomListResponse
import com.heisthunt.shared.dto.StartGameResponse
import com.heisthunt.shared.models.PlayerRole
import com.heisthunt.shared.models.Room
import com.heisthunt.shared.models.RoomSettings

class RoomRepository(private val apiClient: ApiClient) {

    fun getAccessToken(): String? = apiClient.getAccessToken()

    suspend fun createRoom(name: String, settings: RoomSettings = RoomSettings()): Result<Room> {
        return try {
            val response = apiClient.createRoom(CreateRoomRequest(name, settings))
            val room = response.data
            if (response.success && room != null) {
                Result.success(room)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to create room"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRooms(page: Int = 1, pageSize: Int = 20): Result<RoomListResponse> {
        return try {
            val response = apiClient.getRooms(page, pageSize)
            val data = response.data
            if (response.success && data != null) {
                Result.success(data)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to get rooms"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRoom(roomId: String): Result<Room> {
        return try {
            val response = apiClient.getRoom(roomId)
            val room = response.data
            if (response.success && room != null) {
                Result.success(room)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to get room"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinRoom(code: String, password: String? = null): Result<Room> {
        return try {
            val response = apiClient.joinRoom(JoinRoomRequest(code, password))
            val room = response.data
            if (response.success && room != null) {
                Result.success(room)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to join room"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun leaveRoom(roomId: String): Result<Unit> {
        return try {
            val response = apiClient.leaveRoom(roomId)
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to leave room"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleReady(roomId: String): Result<Room> {
        return try {
            val response = apiClient.toggleReady(roomId)
            val room = response.data
            if (response.success && room != null) {
                Result.success(room)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to toggle ready"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun selectRole(roomId: String, role: PlayerRole): Result<Room> {
        return try {
            val response = apiClient.selectRole(roomId, role)
            val room = response.data
            if (response.success && room != null) {
                Result.success(room)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to select role"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun startGame(roomId: String): Result<StartGameResponse> {
        return try {
            val response = apiClient.startGame(roomId)
            val data = response.data
            if (response.success && data != null) {
                Result.success(data)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to start game"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
