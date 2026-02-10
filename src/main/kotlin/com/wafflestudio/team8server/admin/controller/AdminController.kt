package com.wafflestudio.team8server.admin.controller

import com.wafflestudio.team8server.admin.dto.AdminDailyStatsResponse
import com.wafflestudio.team8server.admin.dto.AdminDbStatsResponse
import com.wafflestudio.team8server.admin.dto.AdminReactionTimeHistogramResponse
import com.wafflestudio.team8server.admin.service.AdminService
import com.wafflestudio.team8server.common.auth.LoggedInUserId
import com.wafflestudio.team8server.common.exception.BadRequestException
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

    @Operation(summary = "일별 통계 조회", description = "DAU, 일별 신규 가입자 수, 일별 연습 시도 횟수 등 시계열 통계를 조회합니다. (관리자 전용)")
    @GetMapping("/stats/daily")
    fun getDailyStats(
        @Parameter(hidden = true) @LoggedInUserId userId: Long,
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        from: LocalDate,
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        to: LocalDate,
    ): AdminDailyStatsResponse {
        if (from.isAfter(to)) {
            throw BadRequestException("from must be less than or equal to to")
        }
        return adminService.getDailyStats(from = from, to = to)
    }

    @Operation(
        summary = "반응속도 히스토그램 조회",
        description = "전체 반응속도 데이터를 10ms 구간, 30000ms 상한으로 히스토그램화하여 조회합니다. (관리자 전용)",
    )
    @GetMapping("/stats/reaction-times/histogram")
    fun getReactionTimeHistogram(
        @Parameter(hidden = true) @LoggedInUserId userId: Long,
    ): AdminReactionTimeHistogramResponse = adminService.getReactionTimeHistogram()
}
