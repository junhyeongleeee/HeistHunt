package com.heisthunt.app.di

import com.heisthunt.app.location.LocationService
import com.heisthunt.app.network.ApiClient
import com.heisthunt.app.network.GameWebSocketClient
import com.heisthunt.app.repository.AuthRepository
import com.heisthunt.app.repository.GameRepository
import com.heisthunt.app.repository.RoomRepository
import com.heisthunt.app.viewmodel.AuthViewModel
import com.heisthunt.app.viewmodel.GameViewModel
import com.heisthunt.app.viewmodel.RoomViewModel
import com.heisthunt.shared.models.PlayerRole

object AppModule {
    // API Client - 싱글톤
    private val apiClient: ApiClient by lazy {
        ApiClient(
            baseUrl = getBaseUrl()
        )
    }

    // Repositories
    val authRepository: AuthRepository by lazy {
        AuthRepository(apiClient)
    }

    val roomRepository: RoomRepository by lazy {
        RoomRepository(apiClient)
    }

    val gameRepository: GameRepository by lazy {
        GameRepository(apiClient)
    }

    // ViewModels
    fun provideAuthViewModel(): AuthViewModel {
        return AuthViewModel(authRepository)
    }

    fun provideRoomViewModel(): RoomViewModel {
        return RoomViewModel(
            roomRepository = roomRepository,
            baseUrl = "192.168.1.145:8080" // WebSocket uses host:port format without protocol
        )
    }

    fun provideGameViewModel(
        gameId: String,
        myRole: PlayerRole,
        locationService: LocationService,
        startTime: kotlinx.datetime.Instant?,
        escapeDurationSeconds: Long,
        totalDurationSeconds: Long
    ): GameViewModel {
        val wsClient = GameWebSocketClient(
            baseUrl = "192.168.1.145:8080",
            getAccessToken = { apiClient.getAccessToken() }
        )

        return GameViewModel(
            gameRepository = gameRepository,
            gameId = gameId,
            myRole = myRole,
            locationService = locationService,
            wsClient = wsClient,
            startTime = startTime,
            escapeDurationSeconds = escapeDurationSeconds,
            totalDurationSeconds = totalDurationSeconds
        )
    }

    private fun getBaseUrl(): String {
        // Android Emulator uses 10.0.2.2 to access host localhost
        // iOS Simulator uses localhost directly
        // For physical devices, use your computer's IP address
        return "http://192.168.1.145:8080"
    }
}
