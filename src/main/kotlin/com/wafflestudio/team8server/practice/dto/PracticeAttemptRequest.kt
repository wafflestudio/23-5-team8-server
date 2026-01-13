package com.wafflestudio.team8server.practice.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class PracticeAttemptRequest(
    @field:NotNull(message = "강의 ID는 필수입니다")
    val courseId: Long,
    @field:NotNull(message = "전체 경쟁자 수는 필수입니다")
    @field:Min(value = 1, message = "전체 경쟁자 수는 1 이상이어야 합니다")
    val totalCompetitors: Int,
    @field:NotNull(message = "수강 정원은 필수입니다")
    @field:Min(value = 1, message = "수강 정원은 1 이상이어야 합니다")
    val capacity: Int,
)
