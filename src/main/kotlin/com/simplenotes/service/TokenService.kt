package com.simplenotes.service

import com.simplenotes.configuration.JwtConfiguration
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
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
    jwtConfiguration: JwtConfiguration
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

    private fun extractAllClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
