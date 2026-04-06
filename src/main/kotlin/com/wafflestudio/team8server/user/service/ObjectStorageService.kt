package com.wafflestudio.team8server.user.service

import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.model.CreatePreauthenticatedRequestDetails
import com.oracle.bmc.objectstorage.requests.CreatePreauthenticatedRequestRequest
import com.oracle.bmc.objectstorage.requests.DeleteObjectRequest
import com.wafflestudio.team8server.config.OciStorageProperties
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.util.Date
import java.util.UUID

@Service
@Profile("dev", "prod")
class ObjectStorageService(
    private val objectStorageClient: ObjectStorageClient,
    private val ociStorageProperties: OciStorageProperties,
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

        val expiresAt = Date(System.currentTimeMillis() + ociStorageProperties.presignedUrlExpirationMinutes * 60 * 1000)

        val parDetails =
            CreatePreauthenticatedRequestDetails
                .builder()
                .name("upload-$key")
                .objectName(key)
                .accessType(CreatePreauthenticatedRequestDetails.AccessType.ObjectWrite)
                .timeExpires(expiresAt)
                .build()

        val request =
            CreatePreauthenticatedRequestRequest
                .builder()
                .namespaceName(ociStorageProperties.namespace)
                .bucketName(ociStorageProperties.bucket)
                .createPreauthenticatedRequestDetails(parDetails)
                .build()

        val response = objectStorageClient.createPreauthenticatedRequest(request)
        val parUrl = "https://objectstorage.${ociStorageProperties.region}.oraclecloud.com${response.preauthenticatedRequest.accessUri}"

        return PresignedUrlResult(
            presignedUrl = parUrl,
            imageUrl = buildFullUrl(key),
        )
    }

    fun deleteObject(key: String) {
        val request =
            DeleteObjectRequest
                .builder()
                .namespaceName(ociStorageProperties.namespace)
                .bucketName(ociStorageProperties.bucket)
                .objectName(key)
                .build()

        objectStorageClient.deleteObject(request)
    }

    fun extractKeyFromUrl(imageUrl: String): String {
        val baseUrl = ociStorageProperties.baseUrl.trimEnd('/')
        return imageUrl.removePrefix("$baseUrl/")
    }

    fun buildFullUrl(key: String): String {
        val baseUrl = ociStorageProperties.baseUrl.trimEnd('/')
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
