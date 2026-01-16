package com.wafflestudio.team8server.course.dto

import com.wafflestudio.team8server.common.dto.PageInfo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "강의 검색 응답")
data class CourseSearchResponse(
    @Schema(
        description = "검색 결과 강의 목록",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val items: List<CourseDetailResponse>,
    @Schema(
        description = "pagination 정보",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val pageInfo: PageInfo,
)
