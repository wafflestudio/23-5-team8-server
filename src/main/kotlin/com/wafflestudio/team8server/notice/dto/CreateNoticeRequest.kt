package com.wafflestudio.team8server.notice.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "공지사항 생성 요청")
data class CreateNoticeRequest(
    @field:NotBlank(message = "제목은 필수입니다")
    @Schema(description = "공지사항 제목")
    val title: String,
    @field:NotBlank(message = "내용은 필수입니다")
    @Schema(description = "공지사항 내용")
    val content: String,
    @Schema(description = "고정 공지 여부", defaultValue = "false")
    val isPinned: Boolean = false,
)
