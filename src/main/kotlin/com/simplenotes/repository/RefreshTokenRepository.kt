package com.simplenotes.repository

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.simplenotes.configuration.JwtConfiguration
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RefreshTokenRepository(
    jwtConfiguration: JwtConfiguration
) {
    private val tokens: Cache<String, UserDetails> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMillis(jwtConfiguration.refreshTokenExpiration))
        .build()

    fun findUserByToken(token: String): UserDetails? {
        return tokens.getIfPresent(token)
    }

    fun invalidate(token: String) {
        tokens.invalidate(token)
    }

    fun save(token: String, user: UserDetails) {
        tokens.put(token, user)
    }
}
