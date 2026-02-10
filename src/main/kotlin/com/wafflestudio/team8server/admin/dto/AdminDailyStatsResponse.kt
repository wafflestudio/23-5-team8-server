package com.wafflestudio.team8server.admin.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "일별 통계 응답")
data class AdminDailyStatsResponse(
    @Schema(description = "DAU (일별 1회 이상 연습 시도한 사용자 수)")
    val dailyActiveUsers: List<AdminDailyCountItem>,
    @Schema(description = "일별 신규 가입자 수")
    val dailyNewUsers: List<AdminDailyCountItem>,
    @Schema(description = "일별 연습 시도 횟수 (PracticeDetail 생성 수)")
    val dailyPracticeDetailCounts: List<AdminDailyCountItem>,
)
