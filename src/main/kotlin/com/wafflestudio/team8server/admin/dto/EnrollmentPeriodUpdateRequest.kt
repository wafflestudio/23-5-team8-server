package com.wafflestudio.team8server.admin.dto

import com.wafflestudio.team8server.config.EnrollmentPeriodType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "수강신청 기간 타입 변경 요청")
data class EnrollmentPeriodUpdateRequest(
    @Schema(
        description = "변경할 수강신청 기간 타입",
        example = "FRESHMAN",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val type: EnrollmentPeriodType,
)
