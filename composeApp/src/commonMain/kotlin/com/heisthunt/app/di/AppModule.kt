package com.heisthunt.app.di

import com.heisthunt.app.location.LocationService
import com.heisthunt.app.network.ApiClient
import com.heisthunt.app.network.GameWebSocketClient
import com.heisthunt.app.network.TokenStorage
import com.heisthunt.app.repository.AuthRepository
import com.heisthunt.app.repository.GameRepository
import com.heisthunt.app.repository.RoomRepository
import com.heisthunt.app.storage.SecureStorage
import com.heisthunt.app.utils.HapticFeedback
import com.heisthunt.app.viewmodel.AuthViewModel
import com.heisthunt.app.voice.VoiceChannelManager
import com.heisthunt.app.viewmodel.GameViewModel
import com.heisthunt.app.viewmodel.RoomViewModel
import com.heisthunt.shared.models.Participant
import com.heisthunt.shared.models.PlayerRole
import kotlinx.coroutines.flow.MutableSharedFlow

object AppModule {
    lateinit var secureStorage: SecureStorage
    lateinit var tokenStorage: TokenStorage

    fun initialize(secureStorage: SecureStorage) {
        this.secureStorage = secureStorage
        this.tokenStorage = TokenStorage(secureStorage)
        // Load user from storage on initialization
        this.tokenStorage.loadUser()
    }

    /** Emits Unit when the API client forces a logout due to unrecoverable 401 */
    val forceLogoutFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // API Client - 싱글톤
    private val apiClient: ApiClient by lazy {
        ApiClient(
            baseUrl = getBaseUrl(),
            tokenStorage = tokenStorage
        ).also { client ->
            client.onForceLogout = {
                println("🔒 [AppModule] Force logout triggered by token refresh failure")
                forceLogoutFlow.tryEmit(Unit)
            }
        }
    }

    // Repositories
    val authRepository: AuthRepository by lazy {
        AuthRepository(apiClient, tokenStorage)
    }

    val roomRepository: RoomRepository by lazy {
        RoomRepository(apiClient)
    }

    val gameRepository: GameRepository by lazy {
        GameRepository(apiClient)
    }

    // ViewModels
    fun provideAuthViewModel(): AuthViewModel {
        return AuthViewModel(authRepository, tokenStorage)
    }

    fun provideRoomViewModel(): RoomViewModel {
        return RoomViewModel(
            roomRepository = roomRepository,
            baseUrl = getWebSocketBaseUrl() // WebSocket uses host:port format without protocol
        )
    }

    fun provideGameViewModel(
        gameId: String,
        myRole: PlayerRole,
        participants: List<Participant>,
        myUserId: String,
        locationService: LocationService,
        hapticFeedback: HapticFeedback,
        startTime: kotlinx.datetime.Instant?,
        escapeDurationSeconds: Long,
        totalDurationSeconds: Long,
        voiceChannelManager: VoiceChannelManager? = null
    ): GameViewModel {
        val wsClient = GameWebSocketClient(
            baseUrl = getWebSocketBaseUrl(),
            getAccessToken = { apiClient.getAccessToken() }
        )

        return GameViewModel(
            gameRepository = gameRepository,
            gameId = gameId,
            myRole = myRole,
            participants = participants,
            myUserId = myUserId,
            locationService = locationService,
            wsClient = wsClient,
            hapticFeedback = hapticFeedback,
            startTime = startTime,
            escapeDurationSeconds = escapeDurationSeconds,
            totalDurationSeconds = totalDurationSeconds,
            voiceChannelManager = voiceChannelManager
        )
    }

    private fun getBaseUrl(): String {
        // Android Emulator uses 10.0.2.2 to access host localhost
        // iOS Simulator uses localhost directly
        // For physical devices, use your computer's IP address
        return getPlatformBaseUrl()
    }

    private fun getWebSocketBaseUrl(): String {
        // WebSocket uses host:port format without protocol
        return getPlatformBaseUrl().removePrefix("http://").removePrefix("https://")
    }
}

expect fun getPlatformBaseUrl(): String
