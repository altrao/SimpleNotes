package com.simplenotes.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * @property secret The secret key used for signing and verifying JWT tokens.
 * @property accessTokenExpiration The expiration time for access tokens in milliseconds. Default to 1800000 (30 minutes).
 * @property refreshTokenExpiration The expiration time for refresh tokens in milliseconds. Default to 259200000 (3 days).
 */
@Configuration
@ConfigurationProperties(prefix = "secure-notes.security.jwt")
open class JwtConfiguration(
    var secret: String = "",
    var accessTokenExpiration: Long = 1800000,
    var refreshTokenExpiration: Long = 259_200_000
)
