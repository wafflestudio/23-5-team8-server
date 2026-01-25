package com.wafflestudio.team8server.user.service

import com.wafflestudio.team8server.config.S3Properties
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.time.Duration
import java.util.UUID

@Service
@Profile("prod")
class S3Service(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    private val s3Properties: S3Properties,
) {
    data class PresignedUrlResult(
        val presignedUrl: String,
        val imageUrl: String,
    )

    fun generatePresignedUrl(
        userId: Long,
        extension: String,
        contentType: String,
    ): PresignedUrlResult {
        val key = generateKey(userId, extension)

        val putObjectRequest =
            software.amazon.awssdk.services.s3.model.PutObjectRequest
                .builder()
                .bucket(s3Properties.bucket)
                .key(key)
                .contentType(contentType)
                .build()

        val presignRequest =
            PutObjectPresignRequest
                .builder()
                .signatureDuration(Duration.ofMinutes(s3Properties.presignedUrlExpirationMinutes))
                .putObjectRequest(putObjectRequest)
                .build()

        val presignedRequest = s3Presigner.presignPutObject(presignRequest)
        return PresignedUrlResult(
            presignedUrl = presignedRequest.url().toString(),
            imageUrl = buildFullUrl(key),
        )
    }

    fun deleteObject(key: String) {
        val deleteRequest =
            DeleteObjectRequest
                .builder()
                .bucket(s3Properties.bucket)
                .key(key)
                .build()

        s3Client.deleteObject(deleteRequest)
    }

    fun extractKeyFromUrl(imageUrl: String): String {
        val baseUrl = s3Properties.baseUrl.trimEnd('/')
        return imageUrl.removePrefix("$baseUrl/")
    }

    fun buildFullUrl(key: String): String {
        val baseUrl = s3Properties.baseUrl.trimEnd('/')
        return "$baseUrl/$key"
    }

    private fun generateKey(
        userId: Long,
        extension: String,
    ): String {
        val uuid = UUID.randomUUID().toString()
        return "profiles/$userId/$uuid.$extension"
    }
}
