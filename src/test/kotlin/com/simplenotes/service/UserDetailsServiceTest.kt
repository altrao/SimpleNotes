package com.simplenotes.service

import com.simplenotes.exception.RegisterException
import com.simplenotes.model.AuthenticationRequest
import com.simplenotes.model.Role
import com.simplenotes.model.User
import com.simplenotes.repository.UserRepository
import com.simplenotes.security.UserDetailsService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.given
import org.mockito.kotlin.willReturn
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder

class UserDetailsServiceTest {
    private val userRepository: UserRepository = mock()
    private val passwordEncoder: PasswordEncoder = mock()
    private val userDetailsService = UserDetailsService(userRepository, passwordEncoder)

    @Test
    fun `register should successfully register a new user`() {
        val request = AuthenticationRequest("test@example.com", "P@sswOrd1")
        val encodedPassword = "encodedPassword"
        val savedUser = User(email = "test@example.com", pass = encodedPassword, role = Role.USER)

        given(userRepository.findByEmail(request.username)) willReturn { null }
        given(passwordEncoder.encode(request.password)) willReturn { encodedPassword }
        given(userRepository.createUser(request.username, encodedPassword)) willReturn { savedUser }

        val result = userDetailsService.register(request)

        assertEquals(savedUser.email, result.email)
        assertEquals(savedUser.pass, result.pass)
    }

    @Test
    fun `register should throw RegisterException when user already exists`() {
        val request = AuthenticationRequest("user-exists@email.com", "P@sswOrd1")
        val existingUser = User(email = "user-exists@email.com", pass = "encodedPassword", role = Role.USER)

        given(userRepository.findByEmail(request.username)) willReturn { existingUser }

        assertThrows(RegisterException::class.java) {
            userDetailsService.register(request)
        }
    }

    @Test
    fun `register should throw RegisterException when password is invalid`() {
        val request = AuthenticationRequest("test@example.com", "invalid")

        assertThrows(RegisterException::class.java) {
            userDetailsService.register(request)
        }
    }

    @Test
    fun `register should throw RegisterException when username is invalid`() {
        val request = AuthenticationRequest("invalid", "P@sswOrd1")

        assertThrows(RegisterException::class.java) {
            userDetailsService.register(request)
        }
    }

    @Test
    fun `loadUserByUsername should return UserDetails when user exists`() {
        val username = "test@example.com"
        val persistedUser = User(email = username, pass = "encodedPassword", role = Role.USER)
        given(userRepository.findByEmail(username)) willReturn { persistedUser }

        val userDetails = userDetailsService.loadUserByUsername(username)

        assertEquals(persistedUser.email, userDetails.username)
        assertEquals(persistedUser.pass, userDetails.password)
    }

    @Test
    fun `loadUserByUsername should throw UsernameNotFoundException when user does not exist`() {
        val username = "nonexistent@example.com"
        given(userRepository.findByEmail(username)) willReturn { null }

        assertThrows(UsernameNotFoundException::class.java) {
            userDetailsService.loadUserByUsername(username)
        }
    }
}