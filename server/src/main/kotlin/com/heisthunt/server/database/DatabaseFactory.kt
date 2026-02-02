package com.heisthunt.server.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init(config: ApplicationConfig) {
        val driverClassName = config.property("database.driverClassName").getString()
        val jdbcURL = config.property("database.jdbcURL").getString()
        val username = config.property("database.username").getString()
        val password = config.property("database.password").getString()
        val maxPoolSize = config.propertyOrNull("database.maxPoolSize")?.getString()?.toInt() ?: 10

        val hikariConfig = HikariConfig().apply {
            this.driverClassName = driverClassName
            this.jdbcUrl = jdbcURL
            this.username = username
            this.password = password
            this.maximumPoolSize = maxPoolSize
            this.isAutoCommit = false
            this.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        val dataSource = HikariDataSource(hikariConfig)
        Database.connect(dataSource)

        transaction {
            SchemaUtils.create(
                Users,
                RefreshTokens,
                Rooms,
                RoomParticipants,
                Games,
                GamePlayers,
                LocationUpdates
            )

            // Run migrations
            runMigrations(this)
        }
    }

    private fun runMigrations(transaction: Transaction) {
        // Migration: Add selected_role column to room_participants
        try {
            transaction.exec("ALTER TABLE room_participants ADD COLUMN IF NOT EXISTS selected_role VARCHAR(10)")
        } catch (e: Exception) {
            // Ignore if column already exists or other errors (will be caught by subsequent operations)
            println("Migration warning: ${e.message}")
        }

        // Migration: Add phase and chase_started_at columns to games
        try {
            transaction.exec("ALTER TABLE games ADD COLUMN IF NOT EXISTS phase VARCHAR(20) DEFAULT 'ESCAPE'")
            transaction.exec("ALTER TABLE games ADD COLUMN IF NOT EXISTS chase_started_at TIMESTAMP")
            println("Migration: Added phase and chase_started_at columns to games table")
        } catch (e: Exception) {
            println("Migration warning: ${e.message}")
        }

        // Migration: Add jail location columns to game_players
        try {
            transaction.exec("ALTER TABLE game_players ADD COLUMN IF NOT EXISTS jail_latitude DOUBLE PRECISION")
            transaction.exec("ALTER TABLE game_players ADD COLUMN IF NOT EXISTS jail_longitude DOUBLE PRECISION")
            println("Migration: Added jail_latitude and jail_longitude columns to game_players table")
        } catch (e: Exception) {
            println("Migration warning: ${e.message}")
        }
    }
}
