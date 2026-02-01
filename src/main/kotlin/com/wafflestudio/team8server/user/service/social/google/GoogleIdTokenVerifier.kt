package com.wafflestudio.team8server.user.service.social.google

import com.wafflestudio.team8server.common.exception.UnauthorizedException
import com.wafflestudio.team8server.config.OAuthProperties
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Component

data class GoogleUserInfo(
    val sub: String,
    val email: String?,
    val name: String?,
    val picture: String?,
)

@Component
class GoogleIdTokenVerifier(
    private val props: OAuthProperties,
) {
    private val decoder: JwtDecoder =
        NimbusJwtDecoder.withJwkSetUri(props.google.jwkSetUri).build()

    fun verifyAndExtract(idToken: String): GoogleUserInfo {
        val jwt = decode(idToken)
        validateIssuer(jwt)
        validateAudience(jwt)

        val sub = jwt.subject
        if (sub.isNullOrBlank()) {
            throw UnauthorizedException("구글 토큰 sub가 비어 있습니다")
        }

        return GoogleUserInfo(
            sub = sub,
            email = null, // jwt.getClaimAsString("email"),
            name = jwt.getClaimAsString("name"),
            picture = null, // jwt.getClaimAsString("picture"),
        )
    }

    private fun decode(idToken: String): Jwt =
        try {
            decoder.decode(idToken)
        } catch (e: Exception) {
            throw UnauthorizedException("구글 id_token 검증에 실패했습니다")
        }

    private fun validateIssuer(jwt: Jwt) {
        val issuer = jwt.issuer?.toString()
        if (issuer.isNullOrBlank() || issuer != props.google.issuer) {
            throw UnauthorizedException("구글 토큰 issuer가 올바르지 않습니다")
        }
    }

    private fun validateAudience(jwt: Jwt) {
        val aud = jwt.audience
        if (aud.isNullOrEmpty() || !aud.contains(props.google.clientId)) {
            throw UnauthorizedException("구글 토큰 audience가 올바르지 않습니다")
        }
    }
}
