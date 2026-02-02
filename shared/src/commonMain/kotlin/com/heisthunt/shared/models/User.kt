package com.heisthunt.shared.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val email: String,
    val nickname: String,
    val profileImageUrl: String? = null,
    val stats: UserStats = UserStats(),
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class UserStats(
    val totalGames: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val policeWins: Int = 0,
    val thiefWins: Int = 0
) {
    val winRate: Float
        get() = if (totalGames > 0) wins.toFloat() / totalGames else 0f
}
