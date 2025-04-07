package com.simplenotes.service

import com.simplenotes.configuration.JwtConfiguration
import com.simplenotes.model.AuthenticationRequest
import com.simplenotes.model.AuthenticationResponse
import com.simplenotes.repository.RefreshTokenRepository
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
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtConfiguration: JwtConfiguration
) {
    fun register(authenticationRequest: AuthenticationRequest): AuthenticationResponse {
        val user = userDetailsService.register(authenticationRequest)
        val (accessToken, refreshToken) = generateTokens(user)

        return AuthenticationResponse(accessToken, refreshToken)
    }

    fun authentication(authenticationRequest: AuthenticationRequest): AuthenticationResponse {
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

        return AuthenticationResponse(
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    fun refreshAccessToken(refreshToken: String): AuthenticationResponse {
        val username = tokenService.extractUsername(refreshToken)
        val user = userDetailsService.loadUserByUsername(username)
        val refreshTokenUser = refreshTokenRepository.findUserByToken(refreshToken)

        return if (user.username != null && user.username == refreshTokenUser?.username) {
            val (newAccessToken, newRefreshToken) = generateTokens(user)

            refreshTokenRepository.invalidate(refreshToken)

            AuthenticationResponse(newAccessToken, newRefreshToken)
        } else {
            throw AuthenticationServiceException("Invalid refresh token")
        }
    }

    private fun generateTokens(user: UserDetails): Pair<String, String> {
        val accessToken = generateAccessToken(user)
        val refreshToken = generateRefreshToken(user)

        refreshTokenRepository.save(refreshToken, user)

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
