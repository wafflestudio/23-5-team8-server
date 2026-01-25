package com.wafflestudio.team8server.user.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Presigned URL 응답")
data class PresignedUrlResponse(
    @Schema(
        description = "S3 업로드용 Presigned URL",
        example = "https://bucket.s3.ap-northeast-2.amazonaws.com/profiles/1/uuid.jpg?X-Amz-Algorithm=...",
    )
    val presignedUrl: String,
    @Schema(
        description = "업로드 완료 후 이미지가 저장될 URL",
        example = "https://bucket.s3.ap-northeast-2.amazonaws.com/profiles/1/uuid.jpg",
    )
    val imageUrl: String,
)
