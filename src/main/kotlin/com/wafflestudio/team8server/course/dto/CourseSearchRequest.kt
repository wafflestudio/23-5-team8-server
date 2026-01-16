package com.wafflestudio.team8server.course.dto

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema

data class CourseSearchRequest(
    @Parameter(
        description = "주 검색어 (교과목명/교수명 검색어)",
        example = "자료구조",
        required = false,
    )
    @Schema(nullable = true)
    val query: String? = null,
    @Parameter(
        description = "교과목번호 (예: 4190.310)",
        example = "4190.310",
        required = false,
    )
    @Schema(nullable = true)
    val courseNumber: String? = null,
    @Parameter(
        description = "이수과정 (예: 학사/석사/석박사통합)",
        example = "학사",
        required = false,
    )
    @Schema(nullable = true)
    val academicCourse: String? = null,
    @Parameter(
        description = "학년 (예: 1/2/3/4)",
        example = "3",
        required = false,
    )
    @Schema(nullable = true)
    val academicYear: Int? = null,
    @Parameter(
        description = "개설대학 (예: 공과대학/자연과학대학)",
        example = "공과대학",
        required = false,
    )
    @Schema(nullable = true)
    val college: String? = null,
    @Parameter(
        description = "개설학과 (예: 컴퓨터공학부/전기·정보공학부)",
        example = "컴퓨터공학부",
        required = false,
    )
    @Schema(nullable = true)
    val department: String? = null,
    @Parameter(
        description = "교과구분 (예: 전필/전선/논문)",
        example = "전필",
        required = false,
    )
    @Schema(nullable = true)
    val classification: String? = null,
    @Parameter(
        description = "페이지 (0부터 시작)",
        example = "0",
        required = true,
    )
    @Schema(nullable = false)
    val page: Int = 0,
    @Parameter(
        description = "페이지 크기",
        example = "10",
        required = true,
    )
    @Schema(nullable = false)
    val size: Int = 10,
)
