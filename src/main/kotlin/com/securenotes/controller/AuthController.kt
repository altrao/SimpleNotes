package com.securenotes.com.securenotes.controller

import com.securenotes.com.securenotes.model.AuthenticationRequest
import com.securenotes.com.securenotes.model.AuthenticationResponse
import com.securenotes.com.securenotes.model.RefreshTokenRequest
import com.securenotes.com.securenotes.model.TokenResponse
import com.securenotes.com.securenotes.service.AuthenticationService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authenticationService: AuthenticationService
) {
    @PostMapping
    fun authenticate(@RequestBody authRequest: AuthenticationRequest): AuthenticationResponse {
        return authenticationService.authentication(authRequest)
    }

    @PostMapping("/refresh")
    fun refreshAccessToken(@RequestBody request: RefreshTokenRequest): TokenResponse {
        return TokenResponse(token = authenticationService.refreshAccessToken(request.token))
    }
}