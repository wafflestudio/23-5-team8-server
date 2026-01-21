package com.wafflestudio.team8server.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Schema(description = "회원가입 요청")
data class SignupRequest(
    @field:NotBlank(message = "이메일은 필수입니다")
    @field:Email(message = "유효한 이메일 형식이어야 합니다")
    @Schema(description = "사용자 이메일 주소", example = "user@example.com", required = true)
    val email: String,
    @field:NotBlank(message = "비밀번호는 필수입니다")
    @field:Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다")
    @field:Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@\$!%*#?&])[A-Za-z\\d@\$!%*#?&]+\$",
        message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다",
    )
    @Schema(
        description = "비밀번호 (8-20자, 영문/숫자/특수문자 포함)",
        example = "password123!",
        required = true,
    )
    val password: String,
    @field:Size(min = 2, max = 6, message = "닉네임은 2자 이상 6자 이하여야 합니다")
    @Schema(description = "사용자 닉네임 (2-6자, 미입력 시 랜덤 생성)", example = "홍길동", required = false)
    val nickname: String? = null,
)
