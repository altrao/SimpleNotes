package com.simplenotes.service

import com.simplenotes.configuration.JwtConfiguration
import com.simplenotes.repository.TokenRepository
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.spec.SecretKeySpec


/**
 * Service responsible for generating and validating JWT tokens.
 *
 * Utilizes the HMAC-SHA256 algorithm for signing and verifying JWT tokens.
 */
@Service
class TokenService(
    jwtConfiguration: JwtConfiguration,
    private val tokenRepository: TokenRepository
) {
    private val signingKey: SecretKeySpec = SecretKeySpec(Base64.getDecoder().decode(jwtConfiguration.secret), "HmacSHA256")

    fun generateToken(subject: String, expiration: Date, additionalClaims: Map<String, Any> = emptyMap()): String {
        val claims = additionalClaims.toMutableMap().apply { put("jti", UUID.randomUUID().toString()) }

        return Jwts.builder()
            .claims(claims)
            .subject(subject)
            .issuedAt(Date(System.currentTimeMillis()))
            .expiration(expiration)
            .signWith(signingKey)
            .compact()
    }

    fun extractUsername(token: String): String {
        return extractAllClaims(token).subject
    }

    fun isTokenRevoked(token: String): Boolean {
        return tokenRepository.isAccessTokenRevoked(token)
    }

    private fun extractAllClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    fun findUserByRefreshToken(token: String): UserDetails? {
        return tokenRepository.findUserByRefreshToken(token)
    }

    fun revokeRefreshToken(refreshToken: String) {
        tokenRepository.invalidateRefreshToken(refreshToken)
    }

    fun saveRefreshToken(refreshToken: String, user: UserDetails) {
        tokenRepository.saveRefreshToken(refreshToken, user)
    }

    fun revokeAccessToken(accessToken: String) {
        tokenRepository.invalidateAccessToken(accessToken)
    }
}
