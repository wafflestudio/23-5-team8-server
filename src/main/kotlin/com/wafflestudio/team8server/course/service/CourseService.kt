package com.wafflestudio.team8server.course.service

import com.wafflestudio.team8server.common.dto.PageInfo
import com.wafflestudio.team8server.course.dto.CourseSearchRequest
import com.wafflestudio.team8server.course.dto.CourseSearchResponse
import com.wafflestudio.team8server.course.dto.CourseSummaryResponse
import com.wafflestudio.team8server.course.model.Semester
import com.wafflestudio.team8server.course.repository.CourseRepository
import com.wafflestudio.team8server.course.repository.CourseSpecification
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class CourseService(
    private val courseRepository: CourseRepository,
) {
    fun search(request: CourseSearchRequest): CourseSearchResponse {
        val pageable =
            PageRequest.of(
                request.page,
                request.size,
                Sort.by("courseTitle").ascending(),
            )

        val specification =
            CourseSpecification.search(
                courseTitle = request.courseTitle,
                courseNumber = request.courseNumber,
                academicCourse = request.academicCourse,
                academicYear = request.academicYear,
                college = request.college,
                department = request.department,
                classification = request.classification,
            )

        val page = courseRepository.findAll(specification, pageable)

        val items =
            page.content.map { course ->
                CourseSummaryResponse(
                    id = course.id!!,
                    courseNumber = course.courseNumber,
                    lectureNumber = course.lectureNumber,
                    courseTitle = course.courseTitle,
                    credit = course.credit,
                    placeAndTime = course.placeAndTime,
                )
            }

        return CourseSearchResponse(
            items = items,
            pageInfo =
                PageInfo(
                    page = page.number,
                    size = page.size,
                    totalElements = page.totalElements,
                    totalPages = page.totalPages,
                    hasNext = page.hasNext(),
                ),
        )
    }

    fun import(
        year: Int,
        semester: Semester,
        file: MultipartFile,
    ): Unit = throw UnsupportedOperationException("not implemented yet")
}
