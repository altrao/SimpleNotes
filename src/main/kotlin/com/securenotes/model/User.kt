package com.securenotes.com.securenotes.model

import java.util.*

data class User(
    val id: UUID,
    val name: String,
    val password: String,
    val role: Role
)
