package com.simplenotes.filter

import com.simplenotes.service.TokenService
import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthorizationFilter(
    private val userDetailsService: UserDetailsService,
    private val tokenService: TokenService,
) : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val authorizationHeader: String? = request.getHeader("Authorization")

        if (null != authorizationHeader && hasToken(authorizationHeader)) {
            try {
                val token = extractToken(authorizationHeader)

                if (tokenService.isTokenRevoked(token)) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed")
                    return
                }

                val username = tokenService.extractUsername(token)

                if (SecurityContextHolder.getContext().authentication == null) {
                    val user = userDetailsService.loadUserByUsername(username)

                    if (username == user.username) {
                        val authToken = UsernamePasswordAuthenticationToken(
                            user, null, user.authorities
                        )

                        authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                        SecurityContextHolder.getContext().authentication = authToken
                    }
                }
            } catch (ex: Exception) {
                if (ex !is JwtException && ex !is AuthenticationException) {
                    logger.error("Authentication error", ex)
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication error")
                } else {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed")
                    return
                }
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun hasToken(header: String) = header.startsWith("Bearer ")
    private fun extractToken(header: String) = header.substringAfter("Bearer ")
}
