package com.wafflestudio.team8server.admin.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "당일 통계 항목")
data class DailyCount(
    @Schema(description = "날짜", example = "2025-01-15")
    val date: LocalDate,
    @Schema(description = "건수", example = "42")
    val count: Long,
)

@Schema(description = "당일 통계 응답")
data class DailyStatsResponse(
    @Schema(description = "당일 신규 가입자 수")
    val dailySignups: List<DailyCount>,
    @Schema(description = "당일 연습 시도 횟수 (practice_details 기준)")
    val dailyPracticeAttempts: List<DailyCount>,
    @Schema(description = "당일 활성 사용자 수 (DAU, 연습 시도 사용자 수)")
    val dailyActiveUsers: List<DailyCount>,
)
