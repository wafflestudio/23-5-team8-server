package com.wafflestudio.team8server.practice.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "연습 세션 항목")
data class PracticeSessionItem(
    @Schema(description = "세션 ID", example = "42")
    val id: Long,
    @Schema(description = "연습 시작 시간", example = "2026-01-15T14:30:00")
    val practiceAt: LocalDateTime,
    @Schema(description = "총 시도 횟수", example = "5")
    val totalAttempts: Int,
    @Schema(description = "성공 횟수", example = "3")
    val successCount: Int,
)
