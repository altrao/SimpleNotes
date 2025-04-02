package com.securenotes.com.securenotes.repository

import com.securenotes.com.securenotes.model.Role
import com.securenotes.com.securenotes.model.User
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class UserRepository(
    encoder: PasswordEncoder
) {
    private val users = mutableSetOf(
        User(
            id = UUID.randomUUID(),
            name = "email-1@gmail.com",
            password = encoder.encode("pass1"),
            role = Role.USER,
        ),
        User(
            id = UUID.randomUUID(),
            name = "email-2@gmail.com",
            password = encoder.encode("pass2"),
            role = Role.ADMIN,
        ),
        User(
            id = UUID.randomUUID(),
            name = "email-3@gmail.com",
            password = encoder.encode("pass3"),
            role = Role.USER,
        )
    )

    fun findByUsername(email: String): User? = null
//        users.firstOrNull { it.name == email }
}