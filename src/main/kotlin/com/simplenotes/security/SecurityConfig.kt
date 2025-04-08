package com.simplenotes.security

import com.simplenotes.filter.JwtAuthorizationFilter
import com.simplenotes.filter.RateLimiterFilter
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
import org.springframework.web.cors.CorsConfiguration

@Configuration
open class SecurityConfig {
    @Bean
    open fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager = config.authenticationManager

    @Bean
    open fun authenticationProvider(userDetailsService: UserDetailsService): AuthenticationProvider {
        return DaoAuthenticationProvider().apply {
            setUserDetailsService(userDetailsService)
            setPasswordEncoder(encoder())
        }
    }

    @Bean
    open fun securityFilterChain(
        http: HttpSecurity,
        jwtAuthenticationFilter: JwtAuthorizationFilter,
        authenticationProvider: AuthenticationProvider,
        rateLimiterFilter: RateLimiterFilter
    ): DefaultSecurityFilterChain {
        http.csrf { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers("/api/auth", "/api/auth/refresh", "/api/auth/register", "/error", "/actuator/health")
                    .permitAll()
                    .anyRequest()
                    .fullyAuthenticated()
            }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .cors { session ->
                session.configurationSource { request ->
                    CorsConfiguration().apply {
                        allowedOrigins = listOf("http://localhost:3000", "http://localhost:3001", "https://holy-curious-bream.ngrok-free.app")
                        allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        allowedHeaders = listOf("*")
                        allowCredentials = true
                    }
                }
            }
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(rateLimiterFilter, JwtAuthorizationFilter::class.java)

        return http.build()
    }

    @Bean
    open fun encoder(): PasswordEncoder = BCryptPasswordEncoder()
}