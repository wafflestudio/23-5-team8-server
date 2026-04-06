package com.wafflestudio.team8server.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "oci.storage")
data class OciStorageProperties(
    val namespace: String,
    val region: String,
    val bucket: String,
    val presignedUrlExpirationMinutes: Long = 15,
) {
    val baseUrl: String
        get() = "https://objectstorage.$region.oraclecloud.com/n/$namespace/b/$bucket/o"
}
