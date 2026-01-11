package com.wafflestudio.team8server.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "소셜 로그인 요청")
data class SocialLoginRequest(
    @field:NotBlank(message = "authorization code는 필수입니다")
    @field:Schema(description = "OAuth Authorization Code", example = "4/0AeaYSHA...", required = true)
    val code: String,
    @field:Schema(description = "OAuth Redirect URI", example = "https://example.com/oauth/callback", required = false, nullable = true)
    val redirectUri: String? = null,
)
