package com.wafflestudio.team8server.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

@Schema(description = "로그인 요청")
data class LoginRequest(
    @field:NotBlank(message = "이메일은 필수입니다")
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    @Schema(description = "사용자 이메일 주소", example = "user@example.com", required = true)
    val email: String,
    @field:NotBlank(message = "비밀번호는 필수입니다")
    @Schema(description = "사용자 비밀번호", example = "password123!", required = true)
    val password: String,
)
