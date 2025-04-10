package com.simplenotes.controller

import com.simplenotes.controller.model.AuthenticationRequest
import com.simplenotes.controller.model.Tokens
import com.simplenotes.controller.model.TokenRequest
import com.simplenotes.service.AuthenticationService
import org.springframework.http.MediaType
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
    fun authenticate(@RequestBody authRequest: AuthenticationRequest): Tokens {
        return authenticationService.authentication(authRequest)
    }

    @PostMapping("/refresh")
    fun refreshAccessToken(@RequestBody request: TokenRequest): Tokens {
        return authenticationService.refreshTokens(request.token)
    }

    @PostMapping("/register", consumes = [MediaType.APPLICATION_JSON_VALUE] , produces = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun register(@RequestBody request: AuthenticationRequest): Tokens {
        return authenticationService.register(request)
    }

}