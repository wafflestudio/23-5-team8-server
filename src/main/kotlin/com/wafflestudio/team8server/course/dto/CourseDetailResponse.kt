package com.wafflestudio.team8server.course.dto

import com.wafflestudio.team8server.course.model.Semester
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "강의 상세 정보")
data class CourseDetailResponse(
    @Schema(
        description = "강의 ID",
        example = "123",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val id: Long,
    @Schema(
        description = "개설 연도",
        example = "2026",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val year: Int,
    @Schema(
        description = "개설 학기",
        example = "SPRING/SUMMER/FALL/WINTER",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val semester: Semester,
    @Schema(
        description = "교과구분 (예: 전필/전선/논문)",
        example = "전필",
        nullable = true,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    )
    val classification: String?,
    @Schema(
        description = "개설 대학 (예: 공과대학/자연과학대학)",
        example = "공과대학",
        nullable = true,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    )
    val college: String?,
    @Schema(
        description = "개설 학과 (예: 컴퓨터공학부)",
        example = "컴퓨터공학부",
        nullable = true,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    )
    val department: String?,
    @Schema(
        description = "이수과정 (예: 학사/석사/석박사통합)",
        example = "학사",
        nullable = true,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    )
    val academicCourse: String?,
    @Schema(
        description = "학년 (예: 1/2/3/4)",
        example = "3",
        nullable = true,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    )
    val academicYear: String?,
    @Schema(
        description = "교과목번호",
        example = "4190.310",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val courseNumber: String,
    @Schema(
        description = "강좌번호",
        example = "001",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val lectureNumber: String,
    @Schema(
        description = "교과목명",
        example = "자료구조",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val courseTitle: String,
    @Schema(
        description = "학점 (예: 1/2/3/4)",
        example = "3",
        nullable = true,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    )
    val credit: Int?,
    @Schema(
        description = "주담당교수",
        example = "홍길동",
        nullable = true,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    )
    val instructor: String?,
    @Schema(
        description = "강의실 및 시간",
        example = "3",
        nullable = true,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    )
    val placeAndTime: String?,
    @Schema(
        description = "정원",
        example = "80",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val quota: Int,
    @Schema(
        description = "신입생정원",
        example = "10",
        nullable = true,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    )
    val freshmanQuota: Int?,
)
