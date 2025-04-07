package com.simplenotes.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "secure-notes.pagination")
open class PaginationConfiguration(
    var limit: Int = 1000
)
