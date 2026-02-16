package com.heisthunt.server.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Users : Table("users") {
    val id = varchar("id", 36)
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val nickname = varchar("nickname", 50).uniqueIndex()
    val profileImageUrl = varchar("profile_image_url", 500).nullable()
    val totalGames = integer("total_games").default(0)
    val wins = integer("wins").default(0)
    val losses = integer("losses").default(0)
    val policeWins = integer("police_wins").default(0)
    val thiefWins = integer("thief_wins").default(0)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object RefreshTokens : Table("refresh_tokens") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(Users.id)
    val token = varchar("token", 500).uniqueIndex()
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}

object Rooms : Table("rooms") {
    val id = varchar("id", 36)
    val code = varchar("code", 6).uniqueIndex()
    val name = varchar("name", 100)
    val hostId = varchar("host_id", 36).references(Users.id)
    val maxPlayers = integer("max_players").default(10)
    val policeRatio = float("police_ratio").default(0.3f)
    val gameDurationMinutes = integer("game_duration_minutes").default(30)
    val escapeDurationMinutes = integer("escape_duration_minutes").default(5)
    val password = varchar("password", 100).nullable()
    val status = varchar("status", 20).default("WAITING")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object RoomParticipants : Table("room_participants") {
    val id = varchar("id", 36)
    val roomId = varchar("room_id", 36).references(Rooms.id)
    val userId = varchar("user_id", 36).references(Users.id)
    val isReady = bool("is_ready").default(false)
    val selectedRole = varchar("selected_role", 10).nullable() // 유저가 선택한 역할
    val role = varchar("role", 10).nullable() // 게임 시작 시 할당된 역할
    val joinedAt = timestamp("joined_at")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(roomId, userId)
    }
}

object Games : Table("games") {
    val id = varchar("id", 36)
    val roomId = varchar("room_id", 36).references(Rooms.id)
    val status = varchar("status", 20)
    val phase = varchar("phase", 20).default("ESCAPE") // ESCAPE(5분) -> CHASE
    val winner = varchar("winner", 10).nullable()
    val startedAt = timestamp("started_at")
    val endedAt = timestamp("ended_at").nullable()
    val chaseStartedAt = timestamp("chase_started_at").nullable() // 추격 시작 시간

    override val primaryKey = PrimaryKey(id)
}

object GamePlayers : Table("game_players") {
    val id = varchar("id", 36)
    val gameId = varchar("game_id", 36).references(Games.id)
    val userId = varchar("user_id", 36).references(Users.id)
    val role = varchar("role", 10)
    val isCaught = bool("is_caught").default(false)
    val caughtAt = timestamp("caught_at").nullable()
    val jailLatitude = double("jail_latitude").nullable()
    val jailLongitude = double("jail_longitude").nullable()

    override val primaryKey = PrimaryKey(id)
}

object LocationUpdates : Table("location_updates") {
    val id = varchar("id", 36)
    val gameId = varchar("game_id", 36).references(Games.id)
    val userId = varchar("user_id", 36).references(Users.id)
    val latitude = double("latitude")
    val longitude = double("longitude")
    val accuracy = float("accuracy").nullable()
    val timestamp = timestamp("timestamp")

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, gameId, userId, timestamp)
    }
}
