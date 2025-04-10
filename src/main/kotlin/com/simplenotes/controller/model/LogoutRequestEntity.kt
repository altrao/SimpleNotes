package com.simplenotes.controller.model

data class LogoutRequestEntity(
    val accessToken: String? = null,
    val refreshToken: String? = null
)