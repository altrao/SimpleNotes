package com.securenotes.com.securenotes.security

import com.securenotes.com.securenotes.filter.JwtAuthorizationFilter
import com.securenotes.com.securenotes.repository.UserRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.DefaultSecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
open class SecurityConfig {
    @Bean
    open fun userDetailsService(userRepository: UserRepository): UserDetailsService {
        return UserDetailsService(userRepository)
    }

    @Bean
    open fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager = config.authenticationManager

    @Bean
    open fun authenticationProvider(userRepository: UserRepository): AuthenticationProvider {
        return DaoAuthenticationProvider().apply {
            setUserDetailsService(userDetailsService(userRepository))
            setPasswordEncoder(encoder())
        }
    }

    @Bean
    open fun securityFilterChain(
        http: HttpSecurity,
        jwtAuthenticationFilter: JwtAuthorizationFilter,
        authenticationProvider: AuthenticationProvider
    ): DefaultSecurityFilterChain {
        http.csrf { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers("/api/auth", "/api/auth/refresh", "/error")
                    .permitAll()
                    .anyRequest()
                    .fullyAuthenticated()
            }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    open fun encoder(): PasswordEncoder = BCryptPasswordEncoder()
}