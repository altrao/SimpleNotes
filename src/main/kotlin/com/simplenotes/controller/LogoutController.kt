package com.simplenotes.controller

import com.simplenotes.controller.model.LogoutRequestEntity
import com.simplenotes.model.User
import com.simplenotes.service.TokenService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/logout")
class LogoutController(
    private val tokenService: TokenService
) {
    @PostMapping
    fun logout(
        @AuthenticationPrincipal user: User,
        @RequestBody request: LogoutRequestEntity
    ): ResponseEntity<String> {
        request.accessToken?.let(tokenService::revokeAccessToken)
        request.refreshToken?.let(tokenService::revokeRefreshToken)

        return ResponseEntity.ok("Logged out")
    }
}