package com.wafflestudio.team8server.course.dto

import io.swagger.v3.oas.annotations.media.Schema

data class CourseSearchRequest(
    @Schema(description = "주 검색어(교과목명/교수명)")
    val query: String? = null,
    @Schema(description = "교과목번호")
    val courseNumber: String? = null,
    @Schema(description = "이수과정")
    val academicCourse: String? = null,
    @Schema(description = "학년")
    val academicYear: Int? = null,
    @Schema(description = "개설대학")
    val college: String? = null,
    @Schema(description = "개설학과")
    val department: String? = null,
    @Schema(description = "교과구분")
    val classification: String? = null,
    @Schema(description = "페이지", defaultValue = "0")
    val page: Int = 0,
    @Schema(description = "페이지 크기", defaultValue = "10")
    val size: Int = 10,
)
