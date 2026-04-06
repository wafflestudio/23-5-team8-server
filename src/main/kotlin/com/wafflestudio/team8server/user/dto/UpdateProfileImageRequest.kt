package com.wafflestudio.team8server.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "프로필 이미지 URL 저장 요청")
data class UpdateProfileImageRequest(
    @Schema(
        description = "Object Storage에 업로드된 이미지의 전체 URL",
    )
    @field:NotBlank(message = "이미지 URL은 필수입니다")
    val imageUrl: String,
)
