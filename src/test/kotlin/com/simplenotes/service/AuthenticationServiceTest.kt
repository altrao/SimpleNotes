package com.simplenotes.service

import com.simplenotes.configuration.JwtConfiguration
import com.simplenotes.model.AuthenticationRequest
import com.simplenotes.model.Role
import com.simplenotes.repository.RefreshTokenRepository
import com.simplenotes.security.UserDetailsService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.*
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User

class AuthenticationServiceTest {
    private val authManager: AuthenticationManager = mock()
    private val userDetailsService: UserDetailsService = mock()
    private val tokenService: TokenService = mock()
    private val refreshTokenRepository: RefreshTokenRepository = mock()

    private val jwtConfiguration = JwtConfiguration()

    private val authenticationService = AuthenticationService(
        authManager,
        userDetailsService,
        tokenService,
        refreshTokenRepository,
        jwtConfiguration
    )

    @Test
    fun `register should register user and return authentication response`() {
        val request = AuthenticationRequest("test-user", "password")
        val user = User("test-user", "password", listOf(SimpleGrantedAuthority(Role.USER.name)))
        val authentication = UsernamePasswordAuthenticationToken(user, "password", emptyList())
        val accessToken = "mockedAccessToken"
        val refreshToken = "mockedRefreshToken"

        given(authManager.authenticate(UsernamePasswordAuthenticationToken(request.username, request.password))) willReturn { authentication }
        given(userDetailsService.loadUserByUsername(request.username)) willReturn { user }
        given(userDetailsService.register(request)) willReturn {
            com.simplenotes.model.User(
                email = user.username,
                pass = user.password,
                role = Role.USER
            )
        }
        given(tokenService.generateToken(eq(user.username), any(), argWhere { it.isNotEmpty() })) willReturn { accessToken }
        given(tokenService.generateToken(eq(user.username), any(), argWhere{ it.isEmpty() })) willReturn { refreshToken }

        val response = authenticationService.register(request)

        assertEquals(accessToken, response.accessToken)
        assertEquals(refreshToken, response.refreshToken)
    }

    @Test
    fun `authentication should return authentication response`() {
        val request = AuthenticationRequest("test-user", "password")
        val user = User("test-user", "password", listOf(SimpleGrantedAuthority(Role.USER.name)))
        val authentication = UsernamePasswordAuthenticationToken(user, "password", emptyList())
        val accessToken = "mockedAccessToken"
        val refreshToken = "mockedRefreshToken"

        given(authManager.authenticate(UsernamePasswordAuthenticationToken(request.username, request.password))) willReturn { authentication }
        given(userDetailsService.loadUserByUsername(request.username)) willReturn { user }
        given(tokenService.generateToken(eq(user.username), any(), argWhere { it.isNotEmpty() })) willReturn { accessToken }
        given(tokenService.generateToken(eq(user.username), any(), argWhere { it.isEmpty() })) willReturn { refreshToken }

        val response = authenticationService.authentication(request)

        assertEquals(accessToken, response.accessToken)
        assertEquals(refreshToken, response.refreshToken)
    }

    @Test
    fun `authentication should throw BadCredentialsException for incorrect credentials`() {
        val request = AuthenticationRequest("test-user", "wrongpassword")
        val authentication = UsernamePasswordAuthenticationToken(null, null)

        given(authManager.authenticate(UsernamePasswordAuthenticationToken(request.username, request.password))) willReturn { authentication }

        assertThrows(BadCredentialsException::class.java) {
            authenticationService.authentication(request)
        }
    }

    @Test
    fun `refreshAccessToken should return new access and refresh tokens`() {
        val refreshToken = "oldRefreshToken"
        val username = "test-user"
        val currentUser = User(username, "password", listOf(SimpleGrantedAuthority(Role.USER.name)))
        val refreshTokenUser = User(username, "password", emptyList())
        val newAccessToken = "newAccessToken"
        val newRefreshToken = "newRefreshToken"

        given(tokenService.extractUsername(refreshToken)) willReturn { username }
        given(userDetailsService.loadUserByUsername(username)) willReturn { currentUser }
        given(refreshTokenRepository.findUserByToken(refreshToken)) willReturn { refreshTokenUser }
        given(tokenService.generateToken(eq(currentUser.username), any(), argWhere { it.isNotEmpty() })) willReturn { newAccessToken }
        given(tokenService.generateToken(eq(currentUser.username), any(), argWhere { it.isEmpty() })) willReturn { newRefreshToken }

        val response = authenticationService.refreshAccessToken(refreshToken)

        assertEquals(newAccessToken, response.accessToken)
        assertEquals(newRefreshToken, response.refreshToken)
    }

    @Test
    fun `refreshAccessToken should throw AuthenticationServiceException for invalid refresh token`() {
        val refreshToken = "invalidRefreshToken"
        val username = "test-user"
        val currentUser = User(username, "password", emptyList())
        val refreshTokenUser = User("other-user", "password", emptyList())

        given(tokenService.extractUsername(refreshToken)) willReturn { username }
        given(userDetailsService.loadUserByUsername(username)) willReturn { currentUser }
        given(refreshTokenRepository.findUserByToken(refreshToken)) willReturn { refreshTokenUser }

        assertThrows(AuthenticationServiceException::class.java) {
            authenticationService.refreshAccessToken(refreshToken)
        }
    }
}