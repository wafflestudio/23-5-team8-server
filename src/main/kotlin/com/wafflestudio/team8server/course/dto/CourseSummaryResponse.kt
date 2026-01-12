package com.wafflestudio.team8server.course.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "강의 요약 정보")
data class CourseSummaryResponse (
    val id: Long,
    val courseNumber: String,
    val lectureNumber: String,
    val courseTitle: String,
    val credit: Int,
    val placeAndTime: String?,
)