package com.simplenotes.service

import com.simplenotes.configuration.JwtConfiguration
import com.simplenotes.controller.model.AuthenticationRequest
import com.simplenotes.controller.model.Tokens
import com.simplenotes.security.UserDetailsService
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*


@Service
class AuthenticationService(
    private val authManager: AuthenticationManager,
    private val userDetailsService: UserDetailsService,
    private val tokenService: TokenService,
    private val jwtConfiguration: JwtConfiguration
) {
    fun register(authenticationRequest: AuthenticationRequest): Tokens {
        val user = userDetailsService.register(authenticationRequest)
        val (accessToken, refreshToken) = generateTokens(user)

        return Tokens(accessToken, refreshToken)
    }

    fun authentication(authenticationRequest: AuthenticationRequest): Tokens {
        val authentication = authManager.authenticate(
            UsernamePasswordAuthenticationToken(
                authenticationRequest.username,
                authenticationRequest.password
            )
        )

        if (!authentication.isAuthenticated) {
            throw BadCredentialsException("Invalid username or password")
        }

        val user = userDetailsService.loadUserByUsername(authenticationRequest.username)
        val (accessToken, refreshToken) = generateTokens(user)

        return Tokens(
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    fun refreshTokens(refreshToken: String): Tokens {
        if (tokenService.isTokenRevoked(refreshToken)) {
            throw AuthenticationServiceException("Invalid refresh token")
        }

        val username = tokenService.extractUsername(refreshToken)
        val user = userDetailsService.loadUserByUsername(username)
        val refreshTokenUser = tokenService.findUserByRefreshToken(refreshToken)

        return if (user.username != null && user.username == refreshTokenUser?.username) {
            val (newAccessToken, newRefreshToken) = generateTokens(user)

            tokenService.revokeRefreshToken(refreshToken)

            Tokens(newAccessToken, newRefreshToken)
        } else {
            throw AuthenticationServiceException("Invalid refresh token")
        }
    }

    private fun generateTokens(user: UserDetails): Pair<String, String> {
        val accessToken = generateAccessToken(user)
        val refreshToken = generateRefreshToken(user)

        tokenService.saveRefreshToken(refreshToken, user)

        return accessToken to refreshToken
    }

    private fun generateAccessToken(user: UserDetails): String {
        return tokenService.generateToken(
            subject = user.username,
            expiration = Date.from(Instant.now().plusMillis(jwtConfiguration.accessTokenExpiration)),
            additionalClaims = user.authorities.associate { it.authority to true }
        )
    }

    private fun generateRefreshToken(user: UserDetails): String {
        return tokenService.generateToken(
            subject = user.username,
            expiration = Date.from(Instant.now().plusMillis(jwtConfiguration.refreshTokenExpiration))
        )
    }
}
