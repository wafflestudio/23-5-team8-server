package com.wafflestudio.team8server.course.dto

import com.wafflestudio.team8server.course.model.Semester
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "강의 상세 정보")
data class CourseDetailResponse(
    val id: Long,
    val year: Int,
    val semester: Semester,
    val classification: String?,
    val college: String?,
    val department: String?,
    val academicCourse: String?,
    val academicYear: String?,
    val courseNumber: String,
    val lectureNumber: String,
    val courseTitle: String,
    val credit: Int?,
    val instructor: String?,
    val placeAndTime: String?,
    val quota: Int,
    val freshmanQuota: Int?,
)
