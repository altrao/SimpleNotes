package com.simplenotes.security

import com.simplenotes.exception.RegisterException
import com.simplenotes.controller.model.AuthenticationRequest
import com.simplenotes.model.User
import com.simplenotes.repository.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserDetailsService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) : UserDetailsService {
    val emailRegex = Regex("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$")
    val passwordRegex = Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$")

    fun register(request: AuthenticationRequest): User {
        checkUsernameAndPassword(request)

        val existingUser = userRepository.findByEmail(request.username)

        if (existingUser != null) {
            throw RegisterException("Invalid user.")
        }

        return userRepository.createUser(request.username, passwordEncoder.encode(request.password))
    }

    override fun loadUserByUsername(username: String): UserDetails {
        val persistedUser = userRepository.findByEmail(username) ?: throw UsernameNotFoundException("User not found")
        return User(persistedUser.id, persistedUser.email, persistedUser.pass, persistedUser.role)
    }

    private fun checkUsernameAndPassword(request: AuthenticationRequest) {
        if (!isValidPassword(request.password)) {
            throw RegisterException("Password does not meet the required criteria.")
        }

        if (!isValidUsername(request.username)) {
            throw RegisterException("Invalid email format.")
        }

    }

    private fun isValidUsername(username: String): Boolean {
        return username.matches(emailRegex)
    }

    /**
     * Validates whether the given password meets the predefined criteria:
     * - Minimum 8 characters
     * - Contains at least one digit
     * - Contains at least one uppercase letter
     * - Contains at least one lowercase letter
     *
     * @param password the password string to be validated
     * @return true if the password meets the criteria, false otherwise
     */
    private fun isValidPassword(password: String): Boolean {
        return password.matches(passwordRegex)
    }
}