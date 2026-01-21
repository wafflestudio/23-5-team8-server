package com.wafflestudio.team8server.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "프로필 수정 요청 (변경할 필드만 전송)")
data class UpdateProfileRequest(
    @field:Size(min = 2, max = 6, message = "닉네임은 2자 이상 6자 이하여야 합니다")
    @Schema(description = "변경할 닉네임 (2-6자)", example = "새닉네임")
    val nickname: String?,
    @field:Size(max = 255, message = "프로필 이미지 URL은 255자 이하여야 합니다")
    @Schema(description = "변경할 프로필 이미지 URL", example = "https://example.com/new-profile.jpg")
    val profileImageUrl: String?,
)
