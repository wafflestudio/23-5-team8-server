package com.wafflestudio.team8server.admin.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "일별 통계 아이템")
data class AdminDailyCountItem(
    @Schema(description = "날짜", example = "2026-02-10")
    val date: LocalDate,
    @Schema(description = "집계 값", example = "10")
    val count: Long,
)
