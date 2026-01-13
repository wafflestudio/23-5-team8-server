package com.wafflestudio.team8server.course.dto

import com.wafflestudio.team8server.common.dto.PageInfo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "강의 검색 응답")
data class CourseSearchResponse(
    val items: List<CourseSummaryResponse>,
    val pageInfo: PageInfo,
)
