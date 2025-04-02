package com.securenotes.com.securenotes.security

import com.securenotes.com.securenotes.repository.UserRepository
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException

class UserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByUsername(username) ?: throw UsernameNotFoundException("User $username not found!")

        return User.builder()
            .username(user.name)
            .password(user.password)
            .roles(user.role.name)
            .build()
    }
}