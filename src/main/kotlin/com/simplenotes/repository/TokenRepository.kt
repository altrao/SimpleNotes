package com.simplenotes.repository

import com.simplenotes.configuration.JwtConfiguration
import com.simplenotes.model.User
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class TokenRepository(
    jwtConfiguration: JwtConfiguration,
    private val redisTemplate: RedisTemplate<String, UserDetails>
) {
    private val accessTokenExpiration = Duration.ofMillis(jwtConfiguration.accessTokenExpiration)
    private val refreshTokenExpiration = Duration.ofMillis(jwtConfiguration.refreshTokenExpiration)

    private val revokedUser = User(null, "revoked", "revoked")

    fun findUserByRefreshToken(token: String): UserDetails? {
        return redisTemplate.opsForValue().get(token)
    }

    fun invalidateRefreshToken(token: String) {
        redisTemplate.delete(token)
    }

    fun saveRefreshToken(token: String, user: UserDetails) {
        redisTemplate.opsForValue().set(token, user, refreshTokenExpiration)
    }

    fun isAccessTokenRevoked(token: String): Boolean {
        return redisTemplate.opsForValue().get(token.invalidated()) == revokedUser
    }

    fun invalidateAccessToken(token: String) {
        redisTemplate.opsForValue().set(token.invalidated(), revokedUser, accessTokenExpiration)
    }

    private fun String.invalidated() = "invalidated:$this"
}
