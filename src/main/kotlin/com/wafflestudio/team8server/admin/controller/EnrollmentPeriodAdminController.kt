package com.wafflestudio.team8server.admin.controller

import com.wafflestudio.team8server.admin.dto.EnrollmentPeriodResponse
import com.wafflestudio.team8server.admin.dto.EnrollmentPeriodUpdateRequest
import com.wafflestudio.team8server.common.auth.LoggedInUserId
import com.wafflestudio.team8server.config.EnrollmentPeriodProperties
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Admin", description = "관리자 API")
@RestController
@RequestMapping("/api/admin/enrollment-period")
class EnrollmentPeriodAdminController(
    private val enrollmentPeriodProperties: EnrollmentPeriodProperties,
) {
    @Operation(summary = "수강신청 기간 타입 조회", description = "현재 수강신청 기간 타입을 조회합니다. (관리자 전용)")
    @GetMapping
    fun getEnrollmentPeriod(
        @Parameter(hidden = true) @LoggedInUserId userId: Long,
    ): EnrollmentPeriodResponse = EnrollmentPeriodResponse(type = enrollmentPeriodProperties.type)

    @Operation(summary = "수강신청 기간 타입 변경", description = "수강신청 기간 타입을 변경합니다. (관리자 전용)")
    @PutMapping
    fun updateEnrollmentPeriod(
        @Parameter(hidden = true) @LoggedInUserId userId: Long,
        @RequestBody request: EnrollmentPeriodUpdateRequest,
    ): EnrollmentPeriodResponse {
        enrollmentPeriodProperties.type = request.type
        return EnrollmentPeriodResponse(type = enrollmentPeriodProperties.type)
    }
}
