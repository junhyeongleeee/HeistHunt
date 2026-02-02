package com.heisthunt.app.network

import com.heisthunt.shared.dto.*
import com.heisthunt.shared.models.PlayerRole
import com.heisthunt.shared.models.Room
import com.heisthunt.shared.models.User
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class ApiClient(
    internal val baseUrl: String = "http://10.0.2.2:8080" // Android emulator localhost
) {
    private var accessToken: String? = null
    private var refreshToken: String? = null

    internal val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 10000
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }

    fun setTokens(access: String, refresh: String) {
        accessToken = access
        refreshToken = refresh
    }

    fun clearTokens() {
        accessToken = null
        refreshToken = null
    }

    fun hasTokens(): Boolean = accessToken != null

    fun getAccessToken(): String? = accessToken

    internal fun HttpRequestBuilder.authorize() {
        accessToken?.let {
            header(HttpHeaders.Authorization, "Bearer $it")
        }
    }

    // Auth API
    suspend fun register(request: RegisterRequest): ApiResponse<AuthResponse> {
        return client.post("$baseUrl/api/auth/register") {
            setBody(request)
        }.body()
    }

    suspend fun login(request: LoginRequest): ApiResponse<AuthResponse> {
        return client.post("$baseUrl/api/auth/login") {
            setBody(request)
        }.body()
    }

    suspend fun googleLogin(idToken: String): ApiResponse<AuthResponse> {
        return client.post("$baseUrl/api/auth/google") {
            setBody(GoogleLoginRequest(idToken))
        }.body()
    }

    suspend fun refreshTokens(): ApiResponse<TokenResponse> {
        val token = refreshToken ?: throw IllegalStateException("No refresh token")
        return client.post("$baseUrl/api/auth/refresh") {
            setBody(RefreshTokenRequest(token))
        }.body()
    }

    // User API
    suspend fun getMe(): ApiResponse<User> {
        return client.get("$baseUrl/api/users/me") {
            authorize()
        }.body()
    }

    suspend fun getUser(userId: String): ApiResponse<User> {
        return client.get("$baseUrl/api/users/$userId") {
            authorize()
        }.body()
    }

    // Room API
    suspend fun createRoom(request: CreateRoomRequest): ApiResponse<Room> {
        return client.post("$baseUrl/api/rooms") {
            authorize()
            setBody(request)
        }.body()
    }

    suspend fun getRooms(page: Int = 1, pageSize: Int = 20): ApiResponse<RoomListResponse> {
        return client.get("$baseUrl/api/rooms") {
            authorize()
            parameter("page", page)
            parameter("pageSize", pageSize)
        }.body()
    }

    suspend fun getRoom(roomId: String): ApiResponse<Room> {
        return client.get("$baseUrl/api/rooms/$roomId") {
            authorize()
        }.body()
    }

    suspend fun joinRoom(request: JoinRoomRequest): ApiResponse<Room> {
        return client.post("$baseUrl/api/rooms/join") {
            authorize()
            setBody(request)
        }.body()
    }

    suspend fun leaveRoom(roomId: String): ApiResponse<String> {
        return client.post("$baseUrl/api/rooms/$roomId/leave") {
            authorize()
        }.body()
    }

    suspend fun toggleReady(roomId: String): ApiResponse<Room> {
        return client.post("$baseUrl/api/rooms/$roomId/ready") {
            authorize()
        }.body()
    }

    suspend fun selectRole(roomId: String, role: PlayerRole): ApiResponse<Room> {
        return client.post("$baseUrl/api/rooms/$roomId/select-role") {
            authorize()
            setBody(SelectRoleRequest(role))
        }.body()
    }

    // Game API
    suspend fun startGame(roomId: String): ApiResponse<StartGameResponse> {
        return client.post("$baseUrl/api/games/$roomId/start") {
            authorize()
        }.body()
    }

    // Generic methods for repositories - internal to avoid visibility issues
    internal suspend inline fun <reified T> get(path: String): T {
        return client.get("$baseUrl$path") {
            authorize()
        }.body()
    }

    internal suspend inline fun <reified TRequest, reified TResponse> post(path: String, body: TRequest): TResponse {
        return client.post("$baseUrl$path") {
            authorize()
            setBody(body)
        }.body()
    }
}
