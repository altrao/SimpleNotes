package com.securenote

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.securenotes.Application
import com.securenotes.com.securenotes.model.AuthenticationRequest
import com.securenotes.com.securenotes.model.AuthenticationResponse
import com.securenotes.com.securenotes.model.RefreshTokenRequest
import com.securenotes.com.securenotes.model.TokenResponse
import com.securenotes.com.securenotes.service.TokenService
import io.jsonwebtoken.ExpiredJwtException
import org.mockito.Mockito.*
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import kotlin.test.Test
import kotlin.test.assertNotEquals

@SpringBootTest(classes = [Application::class])
@AutoConfigureMockMvc
@TestPropertySource("classpath:application-test.properties")
class ApplicationIntegrationTest {
    @Value("\${jwt.expiredToken}")
    private lateinit var expiredToken: String

    @Autowired
    private lateinit var mockMvc: MockMvc

//    @MockitoBean
    @MockitoSpyBean
    private lateinit var tokenService: TokenService

    @MockitoBean
    private val userDetailsService: UserDetailsService = mock()

    @MockitoBean
    private val authManager: AuthenticationManager = mock()

    @Test
    fun `access secured endpoint with new token from the refresh token after token expiration`() {
        val authRequest = AuthenticationRequest("email-1@gmail.com", "pass1")
        var jsonRequest = jacksonObjectMapper().writeValueAsString(authRequest)

        var response = mockMvc.perform(
            post("/api/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.refreshToken").isNotEmpty).andReturn().response.contentAsString

        val authResponse = jacksonObjectMapper().readValue(response, AuthenticationResponse::class.java)

        // access secured endpoint
        mockMvc.perform(
            get("/api/hello")
                .header("Authorization", "Bearer ${authResponse.accessToken}")
        )
            .andExpect(status().isOk)
            .andExpect(content().string("Hello, Authorized User!"))

        // simulate access token expiration
        `when`(tokenService.extractUsername(authResponse.accessToken))
            .thenThrow(ExpiredJwtException::class.java)

        mockMvc.perform(
            get("/api/hello")
                .header("Authorization", "Bearer ${authResponse.accessToken}")
        )
            .andExpect(status().isForbidden)

        // create a new access token from the refresh token
        val refreshTokenRequest = RefreshTokenRequest(authResponse.refreshToken)
        jsonRequest = jacksonObjectMapper().writeValueAsString(refreshTokenRequest)

        response = mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").isNotEmpty).andReturn().response.contentAsString

        val newAccessToken = jacksonObjectMapper().readValue<TokenResponse>(response)

        reset(tokenService)

        // access secured endpoint with the new token
        mockMvc.perform(
            get("/api/hello")
                .header("Authorization", "Bearer ${newAccessToken.token}")
        )
            .andExpect(status().isOk)
            .andExpect(content().string("Hello, Authorized User!"))
    }

    @Test
    fun `should return unauthorized for unauthenticated user`() {
        val authRequest = AuthenticationRequest("some-user", "pass1")
        val jsonRequest = jacksonObjectMapper().writeValueAsString(authRequest)

        mockMvc.perform(
            post("/api/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest)
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `refresh token with invalid refresh token should return unauthorized`() {
        val jsonRequest = jacksonObjectMapper().writeValueAsString(RefreshTokenRequest(expiredToken))

        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest)
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return forbidden for tampered refresh token`() {
        val authRequest = AuthenticationRequest("email-1@gmail.com", "pass1")
        var jsonRequest = jacksonObjectMapper().writeValueAsString(authRequest)

        val response = mockMvc.perform(
            post("/api/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.refreshToken").isNotEmpty).andReturn().response.contentAsString

        val authResponse = jacksonObjectMapper().readValue(response, AuthenticationResponse::class.java)

        val refreshTokenRequest = RefreshTokenRequest(authResponse.refreshToken)
        jsonRequest = jacksonObjectMapper().writeValueAsString(refreshTokenRequest)

        given(userDetailsService.loadUserByUsername("email-1@gmail.com"))
            .willReturn(User("email-2@gmail.com", "pass2", ArrayList()))

        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest)
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return forbidden for tampered token`() {
        val authRequest = AuthenticationRequest("email-1@gmail.com", "pass1")
        val jsonRequest = jacksonObjectMapper().writeValueAsString(authRequest)

        val response = mockMvc.perform(
            post("/api/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.refreshToken").isNotEmpty).andReturn().response.contentAsString

        val authResponse = jacksonObjectMapper().readValue(response, AuthenticationResponse::class.java)

        given(userDetailsService.loadUserByUsername("email-1@gmail.com"))
            .willReturn(User("email-2@gmail.com", "pass2", ArrayList()))

        mockMvc.perform(
            get("/api/hello")
                .header("Authorization", "Bearer ${authResponse.accessToken}")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `refresh token rotation should issue a new refresh token`() {
        val authRequest = AuthenticationRequest("testuser", "password")

        given(userDetailsService.loadUserByUsername("testuser")).willReturn(User("testuser", "password", emptySet()))

        given(authManager.authenticate(any())).willReturn(mock(Authentication::class.java))

        val authResult = mockMvc.perform(
            post("/api/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(authRequest))
        ).andExpect(status().isOk).andReturn()

        val authResponse: AuthenticationResponse = jacksonObjectMapper().readValue(authResult.response.contentAsString)
        val initialRefreshToken = authResponse.refreshToken

        val refreshTokenRequest = RefreshTokenRequest(initialRefreshToken)

        val refreshResult = mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(refreshTokenRequest))
        ).andExpect(status().isOk).andReturn()

        val tokenResponse: TokenResponse = jacksonObjectMapper().readValue(refreshResult.response.contentAsString)
        val newRefreshToken = tokenResponse.token

        assertNotEquals(initialRefreshToken, newRefreshToken, "Refresh token should be rotated")

        val refreshTokenRequest2 = RefreshTokenRequest(newRefreshToken)

        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(refreshTokenRequest2))
        ).andExpect(status().isOk)
    }
}