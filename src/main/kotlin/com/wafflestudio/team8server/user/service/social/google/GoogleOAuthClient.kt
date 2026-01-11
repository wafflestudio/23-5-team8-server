package com.wafflestudio.team8server.user.service.social.google

import com.fasterxml.jackson.annotation.JsonProperty
import com.wafflestudio.team8server.common.exception.UnauthorizedException
import com.wafflestudio.team8server.config.OAuthProperties
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate

@Component
class GoogleOAuthClient(
    private val props: OAuthProperties,
) {
    private val restTemplate = RestTemplate()

    fun exchangeCodeForIdToken(
        code: String,
        redirectUri: String?,
    ): String {
        if (redirectUri.isNullOrBlank()) {
            // 구글은 code 교환에 redirectUri가 필수인 경우가 대부분이라 여기서 강제
            throw UnauthorizedException("redirectUri가 누락되었습니다")
        }

        val google = props.google

        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_FORM_URLENCODED
                accept = listOf(MediaType.APPLICATION_JSON)
            }

        val form: MultiValueMap<String, String> =
            LinkedMultiValueMap<String, String>().apply {
                add("grant_type", "authorization_code")
                add("client_id", google.clientId)
                add("client_secret", google.clientSecret)
                add("redirect_uri", redirectUri)
                add("code", code)
            }

        val request = HttpEntity(form, headers)

        try {
            val response =
                restTemplate.exchange(
                    google.tokenUri,
                    HttpMethod.POST,
                    request,
                    GoogleTokenResponse::class.java,
                )

            val body = response.body ?: throw UnauthorizedException("구글 토큰 발급에 실패했습니다")
            val idToken = body.idToken ?: throw UnauthorizedException("구글 id_token이 응답에 포함되지 않았습니다")

            return idToken
        } catch (e: HttpStatusCodeException) {
            throw UnauthorizedException("구글 토큰 발급에 실패했습니다")
        } catch (e: Exception) {
            throw UnauthorizedException("구글 토큰 발급에 실패했습니다")
        }
    }
}

private data class GoogleTokenResponse(
    @JsonProperty("id_token")
    val idToken: String?,
)
