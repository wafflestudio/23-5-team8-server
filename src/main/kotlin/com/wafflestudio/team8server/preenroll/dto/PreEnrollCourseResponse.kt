package com.wafflestudio.team8server.preenroll.dto

import com.wafflestudio.team8server.course.dto.CourseDetailResponse
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "장바구니의 특정 강의 정보")
data class PreEnrollCourseResponse(
    @Schema(description = "장바구니 ID")
    val preEnrollId: Long,
    @Schema(description = "강의 정보")
    val course: CourseDetailResponse,
    @Schema(description = "담은 사람 수")
    val cartCount: Int,
)
