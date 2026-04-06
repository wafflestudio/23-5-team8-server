package com.wafflestudio.team8server.config

import com.oracle.bmc.Region
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider
import com.oracle.bmc.objectstorage.ObjectStorageClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("dev", "prod")
class OciStorageConfig(
    private val ociStorageProperties: OciStorageProperties,
) {
    @Bean
    fun objectStorageClient(): ObjectStorageClient {
        val provider = InstancePrincipalsAuthenticationDetailsProvider.builder().build()
        return ObjectStorageClient
            .builder()
            .region(Region.fromRegionCodeOrId(ociStorageProperties.region))
            .build(provider)
    }
}
