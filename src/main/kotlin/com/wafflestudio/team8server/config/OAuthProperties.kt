package com.wafflestudio.team8server.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "oauth")
data class OAuthProperties(
    val kakao: Kakao,
    val google: Google,
) {
    data class Kakao(
        val clientId: String,
        val clientSecret: String? = null,
        val tokenUri: String,
        val userInfoUri: String,
    )
    data class Google(
        val clientId: String,
        val clientSecret: String,
        val tokenUri: String,
        val issuer: String,
        val jwkSerUri: String,
    )
}