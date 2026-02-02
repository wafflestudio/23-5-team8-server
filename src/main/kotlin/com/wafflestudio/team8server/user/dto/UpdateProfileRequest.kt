package com.wafflestudio.team8server.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "프로필 수정 요청")
data class UpdateProfileRequest(
    @field:NotBlank(message = "닉네임은 필수입니다")
    @field:Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하여야 합니다")
    @Schema(description = "변경할 닉네임 (2-20자)", example = "새닉네임")
    val nickname: String,
)
