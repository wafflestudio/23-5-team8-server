package com.wafflestudio.team8server.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Schema(description = "비밀번호 변경 요청")
data class ChangePasswordRequest(
    @field:NotBlank(message = "현재 비밀번호는 필수입니다")
    @Schema(description = "현재 비밀번호", example = "password123!", required = true)
    val currentPassword: String,
    @field:NotBlank(message = "새 비밀번호는 필수입니다")
    @field:Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다")
    @field:Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@\$!%*#?&])[A-Za-z\\d@\$!%*#?&]+\$",
        message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다",
    )
    @Schema(
        description = "새 비밀번호 (8-20자, 영문/숫자/특수문자 포함)",
        example = "newPassword123!",
        required = true,
    )
    val newPassword: String,
)
