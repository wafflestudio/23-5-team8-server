package com.wafflestudio.team8server.admin.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "DB 통계 응답")
data class AdminDbStatsResponse(
    @Schema(description = "총 사용자 수", example = "100")
    val userCount: Long,
    @Schema(description = "총 연습 상세 수", example = "5000")
    val practiceDetailCount: Long,
)
