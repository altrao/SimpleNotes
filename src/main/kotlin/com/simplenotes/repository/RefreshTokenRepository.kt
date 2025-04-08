package com.simplenotes.repository

import com.simplenotes.configuration.JwtConfiguration
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RefreshTokenRepository(
    jwtConfiguration: JwtConfiguration,
    private val redisTemplate: RedisTemplate<String, UserDetails>
) {
    private val expiration = Duration.ofMillis(jwtConfiguration.refreshTokenExpiration)

    fun findUserByToken(token: String): UserDetails? {
        return redisTemplate.opsForValue().get(token)
    }

    fun invalidate(token: String) {
        redisTemplate.delete(token)
    }

    fun save(token: String, user: UserDetails) {
        redisTemplate.opsForValue().set(token, user, expiration)
    }
}
