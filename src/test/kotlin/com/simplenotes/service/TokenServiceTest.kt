package com.simplenotes.service

import com.simplenotes.configuration.JwtConfiguration
import com.simplenotes.repository.TokenRepository
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.test.util.ReflectionTestUtils
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import javax.crypto.SecretKey

class TokenServiceTest {
    private val secret = "5d280cb4685fdc83acec1fab4a75468017b35b84d43f0daca9476b740ebd732f"
    private val subject = "test-user"
    val jwtConfiguration = JwtConfiguration(secret)
    val redisTemplate = RedisTemplate<String, UserDetails>()
    val tokenRepository = TokenRepository(jwtConfiguration, redisTemplate)
    private val tokenService = TokenService(jwtConfiguration, tokenRepository)

    @Test
    fun `generateToken should create a valid token`() {
        val expiration = Date.from(Instant.now().plus(1, ChronoUnit.HOURS))
        val additionalClaims = mapOf("role" to "user")

        val token = tokenService.generateToken(subject, expiration, additionalClaims)

        assertNotNull(token)

        val claims = Jwts.parser()
            .verifyWith(ReflectionTestUtils.getField(tokenService, "signingKey") as SecretKey)
            .build()
            .parseSignedClaims(token)
            .payload

        assertEquals(subject, claims.subject)
        assertEquals("user", claims["role"])
        assertNotNull(claims["jti"])
        assertTrue(claims.expiration.after(Date.from(Instant.now())))
    }

    @Test
    fun `extractUsername should return the correct username`() {
        val expiration = Date.from(Instant.now().plus(1, ChronoUnit.HOURS))
        val token = tokenService.generateToken(subject, expiration)

        val username = tokenService.extractUsername(token)

        assertEquals(subject, username)
    }

    @Test
    fun `extractUsername should throw exception for invalid token`() {
        val invalidToken = "invalid-token"

        assertThrows<MalformedJwtException> {
            tokenService.extractUsername(invalidToken)
        }
    }

    @Test
    fun `generateToken should create a token with default claims`() {
        val expiration = Date.from(Instant.now().plus(1, ChronoUnit.HOURS))

        val token = tokenService.generateToken(subject, expiration)

        assertNotNull(token)

        val claims = Jwts.parser()
            .verifyWith(ReflectionTestUtils.getField(tokenService, "signingKey") as SecretKey)
            .build()
            .parseSignedClaims(token)
            .payload

        assertEquals(subject, claims.subject)
        assertNotNull(claims["jti"])
        assertTrue(claims.expiration.after(Date.from(Instant.now())))
    }

    @Test
    fun `extractUsername should throw exception for expired token`() {
        val expiration = Date.from(Instant.now().minus(1, ChronoUnit.HOURS))
        val token = tokenService.generateToken(subject, expiration)

        assertThrows<ExpiredJwtException> {
            tokenService.extractUsername(token)
        }
    }
}