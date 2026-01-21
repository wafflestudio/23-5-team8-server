package com.wafflestudio.team8server.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "프로필 이미지 URL 저장 요청")
data class UpdateProfileImageRequest(
    @Schema(
        description = "S3에 업로드된 이미지의 전체 URL",
        example = "https://bucket.s3.ap-northeast-2.amazonaws.com/profiles/1/uuid.jpg",
    )
    @field:NotBlank(message = "이미지 URL은 필수입니다")
    val imageUrl: String,
)
