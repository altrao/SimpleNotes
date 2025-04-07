package com.simplenotes.filter

import com.github.benmanes.caffeine.cache.Caffeine
import com.simplenotes.configuration.RateLimitConfiguration
import io.github.bucket4j.Bucket
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration


/**
 * RateLimiterFilter is responsible for rate limiting incoming requests based on the client's IP address.
 *
 * It employs a caching mechanism to store rate-limiting buckets associated with each IP address.
 * The filter enforces two distinct rate limits:
 *
 * 1. **Sustained Rate Limit:**
 *    - Allows a maximum of [capacity] requests within a [minutes]-minute window.
 *
 * 2. **Burst Rate Limit:**
 *    - Permits a burst of up to [burstCapacity] requests within a [burstSeconds]-second window.
 */
@Component
class RateLimiterFilter(
    private val rateLimitConfiguration: RateLimitConfiguration
) : OncePerRequestFilter() {
    val cache = Caffeine.newBuilder().maximumSize(1000).expireAfterAccess(Duration.ofHours(1)).build<String, Bucket>()

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val ipAddress = request.remoteAddr

        try {
            val bucket = cache.get(ipAddress) {
                Bucket.builder()
                    .addLimit { bandwidth ->
                        bandwidth.capacity(rateLimitConfiguration.capacity)
                            .refillIntervally(rateLimitConfiguration.capacity, Duration.ofMinutes(rateLimitConfiguration.minutes))
                    }
                    .addLimit { bandwidth ->
                        bandwidth.capacity(rateLimitConfiguration.burst.capacity)
                            .refillIntervally(rateLimitConfiguration.burst.capacity, Duration.ofSeconds(rateLimitConfiguration.burst.seconds))
                    }
                    .build()
            }

            if (!bucket.tryConsume(1)) {
                response.status = HttpStatus.TOO_MANY_REQUESTS.value()
                response.writer.write("Too many requests")
            }
        } catch (ex: Exception) {
            logger.error("Error processing request", ex)
            response.status = HttpStatus.INTERNAL_SERVER_ERROR.value()
        }

        filterChain.doFilter(request, response)
    }
}
