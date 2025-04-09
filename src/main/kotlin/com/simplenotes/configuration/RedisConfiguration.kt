package com.simplenotes.configuration

import com.simplenotes.logger
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy
import io.github.bucket4j.distributed.proxy.ClientSideConfig
import io.github.bucket4j.distributed.proxy.ProxyManager
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import org.springframework.boot.autoconfigure.data.redis.RedisProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.security.core.userdetails.UserDetails
import java.time.Duration
import java.util.function.Supplier


@Configuration
open class RedisConfiguration(
    private val rateLimitConfiguration: RateLimitConfiguration,
    private val redisProperties: RedisProperties
) {
    private val logger = logger()

    init {
        logger.info("Redis host: ${redisProperties.host}, port: ${redisProperties.port}")
    }

    @Bean
    open fun connectionFactory(): LettuceConnectionFactory {
        return LettuceConnectionFactory(RedisStandaloneConfiguration(redisProperties.host, redisProperties.port))
    }

    @Bean
    open fun redisTemplate(): RedisTemplate<String, UserDetails> {
        return RedisTemplate<String, UserDetails>().apply {
            connectionFactory = connectionFactory()
        }
    }

    @Bean
    open fun lettuceBasedProxyManager(): ProxyManager<ByteArray> {
        val redisClient = RedisClient.create(RedisURI.Builder.redis(redisProperties.host, redisProperties.port).build())
        val clientSideConfig = ClientSideConfig.getDefault().withExpirationAfterWriteStrategy(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(10)))

        return LettuceBasedProxyManager.builderFor(redisClient)
            .withClientSideConfig(clientSideConfig)
            .build()
    }

    @Bean
    open fun bucketConfiguration(): Supplier<BucketConfiguration> {
        return Supplier {
            BucketConfiguration.builder().apply {
                addLimit { bandwidth ->
                    bandwidth.capacity(rateLimitConfiguration.capacity)
                        .refillIntervally(rateLimitConfiguration.capacity, Duration.ofMinutes(rateLimitConfiguration.minutes))
                }
                addLimit { bandwidth ->
                    bandwidth.capacity(rateLimitConfiguration.burst.capacity)
                        .refillIntervally(rateLimitConfiguration.burst.capacity, Duration.ofSeconds(rateLimitConfiguration.burst.seconds))
                }
            }.build()
        }
    }
}