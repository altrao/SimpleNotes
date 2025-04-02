package com.securenotes.com.securenotes.repository

import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class RefreshTokenRepository {
    private val tokens = ConcurrentHashMap<String, UserDetails>()

    fun findUserDetailsByToken(token: String): UserDetails? = tokens[token]

    fun invalidateRefreshToken(token: String) {
        println("Invalidating refresh token: $token")
        tokens.remove(token)
    }

    fun save(token: String, userDetails: UserDetails) {
        tokens[token] = userDetails
        println("New refresh token saved: $token")
    }
}
