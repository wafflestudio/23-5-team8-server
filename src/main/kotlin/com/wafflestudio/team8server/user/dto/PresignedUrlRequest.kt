package com.wafflestudio.team8server.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern

@Schema(description = "Presigned URL 요청")
data class PresignedUrlRequest(
    @Schema(description = "파일 확장자 (jpg, jpeg, png, gif, webp)", example = "jpg")
    @field:NotBlank(message = "파일 확장자는 필수입니다")
    @field:Pattern(
        regexp = "^(jpg|jpeg|png|gif|webp)$",
        message = "지원하지 않는 파일 형식입니다. jpg, jpeg, png, gif, webp만 허용됩니다",
    )
    val extension: String,
    @Schema(description = "Content-Type", example = "image/jpeg")
    @field:NotBlank(message = "Content-Type은 필수입니다")
    @field:Pattern(
        regexp = "^image/(jpeg|png|gif|webp)$",
        message = "지원하지 않는 Content-Type입니다",
    )
    val contentType: String,
    @Schema(description = "파일 크기 (바이트), 최대 5MB", example = "1048576")
    @field:NotNull(message = "파일 크기는 필수입니다")
    @field:Min(value = 1, message = "파일 크기는 1바이트 이상이어야 합니다")
    @field:Max(value = 5 * 1024 * 1024, message = "파일 크기는 5MB를 초과할 수 없습니다")
    val fileSize: Long,
)
