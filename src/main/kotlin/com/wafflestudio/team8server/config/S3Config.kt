package com.wafflestudio.team8server.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner

@Configuration
class S3Config(
    private val s3Properties: S3Properties,
) {
    @Bean
    fun s3Client(): S3Client =
        S3Client
            .builder()
            .region(Region.of(s3Properties.region))
            .build()

    @Bean
    fun s3Presigner(): S3Presigner =
        S3Presigner
            .builder()
            .region(Region.of(s3Properties.region))
            .build()
}
