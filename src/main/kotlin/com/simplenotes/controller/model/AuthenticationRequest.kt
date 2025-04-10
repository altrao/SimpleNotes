package com.simplenotes.controller.model

data class AuthenticationRequest(
    val username: String,
    val password: String,
): APIRequest