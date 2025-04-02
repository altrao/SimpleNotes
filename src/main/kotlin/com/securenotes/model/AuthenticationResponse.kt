package com.securenotes.com.securenotes.model

data class AuthenticationResponse(
    val accessToken: String,
    val refreshToken: String,
)