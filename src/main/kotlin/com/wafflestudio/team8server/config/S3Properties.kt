package com.wafflestudio.team8server.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Profile

@ConfigurationProperties(prefix = "aws.s3")
@Profile("prod")
data class S3Properties(
    val bucket: String,
    val region: String,
    val baseUrl: String,
    val presignedUrlExpirationMinutes: Long,
)
