package com.simplenotes.controller.model

data class Tokens(
    val accessToken: String,
    val refreshToken: String,
): APIResponse, APIRequest