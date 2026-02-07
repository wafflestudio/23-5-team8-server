package com.wafflestudio.team8server.admin.controller

import com.wafflestudio.team8server.admin.dto.AdminDbStatsResponse
import com.wafflestudio.team8server.admin.dto.DailyStatsResponse
import com.wafflestudio.team8server.admin.service.AdminService
import com.wafflestudio.team8server.common.auth.LoggedInUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Tag(name = "Admin", description = "관리자 API")
@RestController
@RequestMapping("/api/admin")
class AdminController(
    private val adminService: AdminService,
) {
    @Operation(summary = "DB 통계 조회", description = "사용자 수, 연습 상세 수 등 DB 통계를 조회합니다. (관리자 전용)")
    @GetMapping("/stats")
    fun getDbStats(
        @Parameter(hidden = true) @LoggedInUserId userId: Long,
    ): AdminDbStatsResponse = adminService.getDbStats()

    @Operation(summary = "당일 통계 조회", description = "당일 신규 가입자, 연습 시도 횟수, 활성 사용자 수를 조회합니다. (관리자 전용)")
    @GetMapping("/stats/daily")
    fun getDailyStats(
        @Parameter(hidden = true) @LoggedInUserId userId: Long,
        @Parameter(description = "조회 시작 날짜", example = "2025-01-01")
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        startDate: LocalDate,
        @Parameter(description = "조회 종료 날짜", example = "2025-01-31")
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        endDate: LocalDate,
    ): DailyStatsResponse = adminService.getDailyStats(startDate, endDate)
}
