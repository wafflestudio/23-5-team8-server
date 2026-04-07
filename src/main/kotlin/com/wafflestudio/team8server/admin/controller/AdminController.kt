package com.wafflestudio.team8server.admin.controller

import com.wafflestudio.team8server.admin.dto.AdminDailyStatsResponse
import com.wafflestudio.team8server.admin.dto.AdminDbStatsResponse
import com.wafflestudio.team8server.admin.dto.AdminReactionTimeHistogramResponse
import com.wafflestudio.team8server.admin.dto.ReactionTimeByAttributeItem
import com.wafflestudio.team8server.admin.dto.ReactionTimeByCourseNumberItem
import com.wafflestudio.team8server.admin.service.AdminService
import com.wafflestudio.team8server.common.auth.LoggedInUserId
import com.wafflestudio.team8server.common.exception.BadRequestException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Tag(name = "Admin", description = "관리자 API")
@SecurityRequirement(name = "Bearer Authentication")
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

    @Operation(summary = "이수구분별 반응속도 통계 조회", description = "이수구분(전공필수, 전공선택, 교양 등)별 반응속도 통계를 조회합니다. (관리자 전용)")
    @GetMapping("/stats/reaction-times/by-classification")
    fun getReactionTimeByClassification(
        @Parameter(hidden = true) @LoggedInUserId userId: Long,
    ): List<ReactionTimeByAttributeItem> = adminService.getReactionTimeByClassification()

    @Operation(summary = "단과대학별 반응속도 통계 조회", description = "단과대학별 반응속도 통계를 조회합니다. (관리자 전용)")
    @GetMapping("/stats/reaction-times/by-college")
    fun getReactionTimeByCollege(
        @Parameter(hidden = true) @LoggedInUserId userId: Long,
    ): List<ReactionTimeByAttributeItem> = adminService.getReactionTimeByCollege()

    @Operation(summary = "학과별 반응속도 통계 조회", description = "학과별 반응속도 통계를 조회합니다. (관리자 전용)")
    @GetMapping("/stats/reaction-times/by-department")
    fun getReactionTimeByDepartment(
        @Parameter(hidden = true) @LoggedInUserId userId: Long,
    ): List<ReactionTimeByAttributeItem> = adminService.getReactionTimeByDepartment()

    @Operation(summary = "과정별 반응속도 통계 조회", description = "과정(학사, 석사, 박사 등)별 반응속도 통계를 조회합니다. (관리자 전용)")
    @GetMapping("/stats/reaction-times/by-academic-course")
    fun getReactionTimeByAcademicCourse(
        @Parameter(hidden = true) @LoggedInUserId userId: Long,
    ): List<ReactionTimeByAttributeItem> = adminService.getReactionTimeByAcademicCourse()

    @Operation(summary = "학년별 반응속도 통계 조회", description = "학년별 반응속도 통계를 조회합니다. (관리자 전용)")
    @GetMapping("/stats/reaction-times/by-academic-year")
    fun getReactionTimeByAcademicYear(
        @Parameter(hidden = true) @LoggedInUserId userId: Long,
    ): List<ReactionTimeByAttributeItem> = adminService.getReactionTimeByAcademicYear()

    @Operation(summary = "학점별 반응속도 통계 조회", description = "학점별 반응속도 통계를 조회합니다. (관리자 전용)")
    @GetMapping("/stats/reaction-times/by-credit")
    fun getReactionTimeByCredit(
        @Parameter(hidden = true) @LoggedInUserId userId: Long,
    ): List<ReactionTimeByAttributeItem> = adminService.getReactionTimeByCredit()

    @Operation(summary = "교과목번호별 반응속도 통계 조회", description = "교과목번호별 반응속도 통계를 조회합니다. (관리자 전용)")
    @GetMapping("/stats/reaction-times/by-course-number")
    fun getReactionTimeByCourseNumber(
        @Parameter(hidden = true) @LoggedInUserId userId: Long,
    ): List<ReactionTimeByCourseNumberItem> = adminService.getReactionTimeByCourseNumber()
}
