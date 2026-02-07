package com.wafflestudio.team8server.admin.controller

import com.wafflestudio.team8server.admin.dto.AdminDbStatsResponse
import com.wafflestudio.team8server.admin.service.AdminService
import com.wafflestudio.team8server.common.auth.LoggedInUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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
}
