package com.wafflestudio.team8server.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aws.s3")
data class S3Properties(
    val bucket: String,
    val region: String,
    val baseUrl: String,
    val presignedUrlExpirationMinutes: Long,
)
