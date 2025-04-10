package com.simplenotes

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.simplenotes.controller.model.APIRequest
import com.simplenotes.controller.model.AuthenticationRequest
import com.simplenotes.controller.model.Tokens
import com.simplenotes.controller.model.TokenRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import kotlin.test.Test
import kotlin.test.assertNotEquals

@AutoConfigureMockMvc
@SpringBootTest(classes = [Application::class])
class ApplicationIntegrationTest {
    private val expiredToken =
        "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImlhdCI6MTc0MzYzNDIyNSwiZXhwIjoxNzQzNjM0Mjg1fQ._1a7SxGQ7gSF57pOaX_A1JDqGQZKCERyTK4PtxPPbDY"
    private val validPassword = "aP@ssw0rd"

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `should return forbidden for expired access token`() {
        mockMvc.perform(
            get("/api/hello").header("Authorization", "Bearer $expiredToken")
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return unauthorized for invalid credentials`() {
        val authRequest = AuthenticationRequest("some-user@email.com", "test-password")

        mockMvc.perform(
            post("/api/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .content(authRequest.asString())
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return unauthorized for invalid refresh token`() {
        val jsonRequest = jacksonObjectMapper().writeValueAsString(TokenRequest(expiredToken))

        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest)
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return unauthorized for tampered refresh token`() {
        val authRequest = AuthenticationRequest("user-tampered-refresh-token@email.com", validPassword)
        val authResponse = authRequest.registerUser()

        val tamperedToken = authResponse.refreshToken.replaceRange(5, 6, "X")

        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TokenRequest(tamperedToken).asString())
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return forbidden for tampered access token`() {
        val authRequest = AuthenticationRequest("user-tampered-access-token@email.com", validPassword)
        val auth = authRequest.registerUser()

        val response = mockMvc.perform(
            post("/api/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .content(AuthenticationRequest(authRequest.username, authRequest.password).asString())
        ).andExpectAll(
            status().isOk,
            jsonPath("$.accessToken").isNotEmpty,
            jsonPath("$.refreshToken").isNotEmpty
        ).andReturn().response.contentAsString

        val authResponse = jacksonObjectMapper().readValue(response, Tokens::class.java)

        // Tamper with the token (e.g., change a character)
        val tamperedToken = authResponse.accessToken.replaceRange(5, 6, "X")

        mockMvc.perform(
            get("/api/hello").header("Authorization", "Bearer $tamperedToken")
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `should issue new refresh token`() {
        val authRequest = AuthenticationRequest("user-new-refresh-token@email.com", validPassword)
        val authResponse = authRequest.registerUser()

        // Refresh tokens
        val refreshResponse = mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TokenRequest(authResponse.refreshToken).asString())
        ).andExpect(status().isOk).andReturn().parseTo<Tokens>()

        // Should refresh new token
        val newAuthResponse = mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TokenRequest(refreshResponse.refreshToken).asString())
        ).andExpect(status().isOk).andReturn().parseTo<Tokens>()

        assertNotEquals(authResponse.refreshToken, refreshResponse.refreshToken, "Refresh token should be rotated")
        assertNotEquals(refreshResponse.refreshToken, newAuthResponse.refreshToken, "Refresh token should be rotated")
    }

    @Test
    fun `should register a new user successfully`() {
        mockMvc.perform(
            asyncDispatch(
                mockMvc.perform(
                    post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(AuthenticationRequest("user-register@email.com", validPassword).asString())
                ).andExpect(request().asyncStarted()).andReturn()
            )
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.refreshToken").isNotEmpty)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `should return bad request when trying to register an already existing user`() {
        AuthenticationRequest("user-register-existing@email.com", validPassword).apply { registerUser() }

        mockMvc.perform(
            asyncDispatch(
                mockMvc.perform(
                    post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(AuthenticationRequest("user-register-existing", validPassword).asString())
                ).andExpect(request().asyncStarted()).andReturn()
            )
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `should return bad request for invalid password`() {
        val authRequest = AuthenticationRequest("new-user@email.com", "weak")

        mockMvc.perform(
            asyncDispatch(
                mockMvc.perform(
                    post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authRequest.asString())
                ).andExpect(request().asyncStarted()).andReturn()
            )
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `should return bad request for empty username`() {
        val authRequest = AuthenticationRequest("", "NewPassword123")

        mockMvc.perform(
            asyncDispatch(
                mockMvc.perform(
                    post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authRequest.asString())
                ).andExpect(request().asyncStarted()).andReturn()
            )
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `should return bad request for empty password`() {
        val authRequest = AuthenticationRequest("user-register-empty-password@email.com", "")

        mockMvc.perform(
            asyncDispatch(
                mockMvc.perform(
                    post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authRequest.asString())
                ).andExpect(request().asyncStarted()).andReturn()
            )
        ).andExpect(status().isBadRequest)
    }

    private fun AuthenticationRequest.registerUser(): Tokens {
        return mockMvc.perform(
            asyncDispatch(
                mockMvc.perform(
                    post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asString())
                ).andExpect(request().asyncStarted()).andReturn()
            )
        ).andExpect(status().isOk).andReturn().parseTo()
    }

    private fun APIRequest.asString(): String {
        return jacksonObjectMapper().writeValueAsString(this)
    }

    private inline fun <reified T> MvcResult.parseTo(): T {
        return jacksonObjectMapper().readValue(this.response.contentAsString)
    }
}
