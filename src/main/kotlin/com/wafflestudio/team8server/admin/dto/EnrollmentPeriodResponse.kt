package com.wafflestudio.team8server.admin.dto

import com.wafflestudio.team8server.config.EnrollmentPeriodType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "현재 수강신청 기간 타입 응답")
data class EnrollmentPeriodResponse(
    @Schema(
        description = "수강신청 기간 타입",
        example = "REGULAR",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val type: EnrollmentPeriodType,
)
