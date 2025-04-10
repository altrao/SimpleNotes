package com.simplenotes.controller

import com.simplenotes.controller.model.LogoutRequestEntity
import com.simplenotes.model.User
import com.simplenotes.service.TokenService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.util.*
import kotlin.test.assertEquals

class LogoutControllerTest {
    private val tokenService: TokenService = mock()
    private val logoutController = LogoutController(tokenService)

    private val user = User(UUID.randomUUID())

    @Test
    fun `logout should revoke both tokens when provided`() {
        val accessToken = "validAccessToken"
        val refreshToken = "validRefreshToken"
        val request = LogoutRequestEntity(accessToken, refreshToken)

        val response = logoutController.logout(user, request)

        assertEquals("Logged out", response.body)
        verify(tokenService).revokeAccessToken(accessToken)
        verify(tokenService).revokeRefreshToken(refreshToken)
    }

    @Test
    fun `logout should revoke only access token when only access token is provided`() {
        val accessToken = "validAccessToken"
        val request = LogoutRequestEntity(accessToken = accessToken, refreshToken = null)

        val response = logoutController.logout(user, request)

        assertEquals("Logged out", response.body)
        verify(tokenService).revokeAccessToken(accessToken)
        verify(tokenService, never()).revokeRefreshToken(any())
    }

    @Test
    fun `logout should revoke only refresh token when only refresh token is provided`() {
        val refreshToken = "validRefreshToken"
        val request = LogoutRequestEntity(accessToken = null, refreshToken = refreshToken)

        val response = logoutController.logout(user, request)

        assertEquals("Logged out", response.body)
        verify(tokenService, never()).revokeAccessToken(any())
        verify(tokenService).revokeRefreshToken(refreshToken)
    }
}