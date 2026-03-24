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
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

class ApiClient(
    internal val baseUrl: String = "http://10.0.2.2:8080",
    private val tokenStorage: TokenStorage
) {
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

    /** Invoked when token refresh fails and the user must be forcefully logged out */
    var onForceLogout: (() -> Unit)? = null

    private val refreshMutex = Mutex()

    fun setTokens(access: String, refresh: String) {
        tokenStorage.accessToken = access
        tokenStorage.refreshToken = refresh
    }

    fun clearTokens() {
        tokenStorage.clear()
    }

    fun hasTokens(): Boolean = tokenStorage.accessToken != null

    fun getAccessToken(): String? = tokenStorage.accessToken

    internal fun HttpRequestBuilder.authorize() {
        tokenStorage.accessToken?.let {
            header(HttpHeaders.Authorization, "Bearer $it")
        }
    }

    /**
     * Attempts to refresh the access token using the stored refresh token.
     * Returns true on success, false otherwise.
     */
    private suspend fun attemptTokenRefresh(): Boolean {
        val refreshToken = tokenStorage.refreshToken ?: run {
            println("❌ [ApiClient] Token refresh failed: no refresh token stored")
            return false
        }
        return try {
            println("🔄 [ApiClient] Refreshing access token")
            val response = client.post("$baseUrl/api/auth/refresh") {
                contentType(ContentType.Application.Json)
                setBody(RefreshTokenRequest(refreshToken))
            }
            val result = response.body<ApiResponse<TokenResponse>>()
            val data = result.data
            if (result.success && data != null) {
                tokenStorage.updateTokens(data.accessToken, data.refreshToken)
                println("✅ [ApiClient] Token refresh success")
                true
            } else {
                println("❌ [ApiClient] Token refresh rejected: ${result.error?.message}")
                false
            }
        } catch (e: Exception) {
            println("❌ [ApiClient] Token refresh exception: ${e.message}")
            false
        }
    }

    /**
     * Wraps an authenticated request with 401 retry logic.
     *
     * On 401 Unauthorized:
     *   1. Uses a Mutex to serialize concurrent refresh attempts (rotation token safety)
     *   2. If another coroutine already refreshed the token, skips refresh and retries
     *   3. Retries the original request after a successful refresh
     *   4. Clears tokens and invokes onForceLogout if refresh fails
     *
     * Auth endpoints under /api/auth/ bypass retry to prevent infinite loops.
     * Uses status-code-based detection (Ktor 3.x does not throw ResponseException for 4xx by default).
     */
    private suspend fun executeWithRetry(
        path: String,
        block: suspend () -> HttpResponse
    ): HttpResponse {
        val response = block()
        if (response.status != HttpStatusCode.Unauthorized || path.startsWith("/api/auth/")) {
            return response
        }

        println("🔄 [ApiClient] 401 received for $path, attempting token refresh")
        val tokenBefore = tokenStorage.accessToken

        val refreshed = refreshMutex.withLock {
            // If token already changed while waiting for lock, a concurrent request
            // already refreshed it — skip refresh and just retry
            if (tokenStorage.accessToken != tokenBefore) {
                println("🔄 [ApiClient] Token already refreshed by concurrent request")
                true
            } else {
                attemptTokenRefresh()
            }
        }

        if (!refreshed) {
            println("❌ [ApiClient] Token refresh failed, clearing tokens and forcing logout")
            tokenStorage.clear()
            onForceLogout?.invoke()
            return response
        }

        println("🔄 [ApiClient] Retrying request after token refresh: $path")
        return block()
    }

    // ─── Auth API (no retry — these endpoints set the tokens) ──────────────────

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
        val token = tokenStorage.refreshToken ?: throw IllegalStateException("No refresh token")
        return client.post("$baseUrl/api/auth/refresh") {
            setBody(RefreshTokenRequest(token))
        }.body()
    }

    // ─── User API ───────────────────────────────────────────────────────────────

    suspend fun getMe(): ApiResponse<User> {
        return executeWithRetry("/api/users/me") {
            client.get("$baseUrl/api/users/me") { authorize() }
        }.body()
    }

    suspend fun getUser(userId: String): ApiResponse<User> {
        return executeWithRetry("/api/users/$userId") {
            client.get("$baseUrl/api/users/$userId") { authorize() }
        }.body()
    }

    // ─── Room API ───────────────────────────────────────────────────────────────

    suspend fun createRoom(request: CreateRoomRequest): ApiResponse<Room> {
        return executeWithRetry("/api/rooms") {
            client.post("$baseUrl/api/rooms") {
                authorize()
                setBody(request)
            }
        }.body()
    }

    suspend fun getRooms(page: Int = 1, pageSize: Int = 20): ApiResponse<RoomListResponse> {
        return executeWithRetry("/api/rooms") {
            client.get("$baseUrl/api/rooms") {
                authorize()
                parameter("page", page)
                parameter("pageSize", pageSize)
            }
        }.body()
    }

    suspend fun getRoom(roomId: String): ApiResponse<Room> {
        return executeWithRetry("/api/rooms/$roomId") {
            client.get("$baseUrl/api/rooms/$roomId") { authorize() }
        }.body()
    }

    suspend fun joinRoom(request: JoinRoomRequest): ApiResponse<Room> {
        return executeWithRetry("/api/rooms/join") {
            client.post("$baseUrl/api/rooms/join") {
                authorize()
                setBody(request)
            }
        }.body()
    }

    suspend fun leaveRoom(roomId: String): ApiResponse<String> {
        return executeWithRetry("/api/rooms/$roomId/leave") {
            client.post("$baseUrl/api/rooms/$roomId/leave") { authorize() }
        }.body()
    }

    suspend fun toggleReady(roomId: String): ApiResponse<Room> {
        return executeWithRetry("/api/rooms/$roomId/ready") {
            client.post("$baseUrl/api/rooms/$roomId/ready") { authorize() }
        }.body()
    }

    suspend fun selectRole(roomId: String, role: PlayerRole): ApiResponse<Room> {
        return executeWithRetry("/api/rooms/$roomId/select-role") {
            client.post("$baseUrl/api/rooms/$roomId/select-role") {
                authorize()
                setBody(SelectRoleRequest(role))
            }
        }.body()
    }

    // ─── Game API ───────────────────────────────────────────────────────────────

    suspend fun startGame(roomId: String): ApiResponse<StartGameResponse> {
        return executeWithRetry("/api/games/$roomId/start") {
            client.post("$baseUrl/api/games/$roomId/start") { authorize() }
        }.body()
    }

    suspend fun leaveGame(gameId: String): ApiResponse<String> {
        return executeWithRetry("/api/games/$gameId/leave") {
            client.post("$baseUrl/api/games/$gameId/leave") { authorize() }
        }.body()
    }

    // ─── Generic methods for repositories ──────────────────────────────────────

    internal suspend inline fun <reified T> get(path: String): T {
        return executeWithRetry(path) {
            client.get("$baseUrl$path") { authorize() }
        }.body()
    }

    internal suspend inline fun <reified TRequest, reified TResponse> post(
        path: String,
        body: TRequest
    ): TResponse {
        return executeWithRetry(path) {
            client.post("$baseUrl$path") {
                authorize()
                setBody(body)
            }
        }.body()
    }
}
