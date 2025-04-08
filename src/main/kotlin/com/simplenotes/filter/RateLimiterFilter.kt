package com.simplenotes.filter

import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.distributed.proxy.ProxyManager
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.function.Supplier


/**
 * RateLimiterFilter is responsible for rate limiting incoming requests based on the client's IP address.
 *
 * It employs Redis to store rate-limiting buckets associated with each IP address.
 * The filter enforces two distinct rate limits:
 *
 * 1. **Sustained Rate Limit:**
 *    - Allows a maximum of [capacity] requests within a [minutes]-minute window.
 *
 * 2. **Burst Rate Limit:**
 *    - Permits a burst of up to [burstCapacity] requests within a [burstSeconds]-second window.
 *
 * Error Handling Behavior:
 * - Returns HTTP 429 (Too Many Requests) when rate limit is exceeded
 * - Continues processing on internal errors (HTTP 500) to avoid service disruption
 */
@Component
class RateLimiterFilter(
    private val bucketConfiguration: Supplier<BucketConfiguration>,
    private val proxyManager: ProxyManager<ByteArray>
) : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val ipAddress = request.remoteAddr

        logger.info("$ipAddress | Calling on ${request.requestURI}")

        try {
            val bucket = proxyManager.builder().build(ipAddress.toByteArray(), bucketConfiguration)
            val consumptionProbe = bucket.tryConsumeAndReturnRemaining(1)

            if (!consumptionProbe.isConsumed) {
                logger.warn("Rate limiting exceeded for IP ${request.remoteAddr} | Calling on ${request.requestURI}")
                response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Too many requests")
                return
            }
        } catch (ex: Exception) {
            logger.error("Rate limiting error for IP ${request.remoteAddr}: ${ex.message}", ex)
            response.status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        }

        filterChain.doFilter(request, response)
    }
}
