package com.heisthunt.server.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.heisthunt.server.plugins.JwtConfig
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

class JwtService(private val config: JwtConfig) {

    fun generateAccessToken(userId: String, email: String): String {
        val expiresAt = Clock.System.now() + config.accessTokenExpireMs.milliseconds
        return JWT.create()
            .withAudience(config.audience)
            .withIssuer(config.issuer)
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withClaim("type", "access")
            .withExpiresAt(Date.from(expiresAt.toJavaInstant()))
            .sign(Algorithm.HMAC256(config.secret))
    }

    fun generateRefreshToken(userId: String): String {
        val expiresAt = Clock.System.now() + config.refreshTokenExpireMs.milliseconds
        return JWT.create()
            .withAudience(config.audience)
            .withIssuer(config.issuer)
            .withClaim("userId", userId)
            .withClaim("type", "refresh")
            .withJWTId(UUID.randomUUID().toString())
            .withExpiresAt(Date.from(expiresAt.toJavaInstant()))
            .sign(Algorithm.HMAC256(config.secret))
    }

    fun verifyRefreshToken(token: String): String? {
        return try {
            val verifier = JWT.require(Algorithm.HMAC256(config.secret))
                .withAudience(config.audience)
                .withIssuer(config.issuer)
                .withClaim("type", "refresh")
                .build()
            val decoded = verifier.verify(token)
            decoded.getClaim("userId").asString()
        } catch (e: Exception) {
            null
        }
    }

    fun getRefreshTokenExpiresAt(): kotlinx.datetime.Instant {
        return Clock.System.now() + config.refreshTokenExpireMs.milliseconds
    }
}
