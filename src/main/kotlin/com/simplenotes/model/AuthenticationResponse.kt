package com.simplenotes.model

data class AuthenticationResponse(
    val accessToken: String,
    val refreshToken: String,
): APIResponse