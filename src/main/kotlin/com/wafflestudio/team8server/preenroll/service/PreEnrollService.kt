package com.wafflestudio.team8server.preenroll.service

import com.wafflestudio.team8server.course.dto.CourseDetailResponse
import com.wafflestudio.team8server.preenroll.dto.PreEnrollCourseResponse
import com.wafflestudio.team8server.preenroll.repository.PreEnrollRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PreEnrollService(
    private val preEnrollRepository: PreEnrollRepository,
) {
    @Transactional(readOnly = true)
    fun getPreEnrolls(
        userId: Long,
        overQuotaOnly: Boolean,
    ): List<PreEnrollCourseResponse> {
        val items = preEnrollRepository.findAllByUserId(userId)

        val filtered =
            if (overQuotaOnly) {
                items.filter { it.cartCount > it.course.quota }
            } else {
                items
            }

        return filtered
            .sortedWith(compareBy({ it.course.courseNumber }, { it.course.lectureNumber }))
            .map { preEnroll ->
                val course = preEnroll.course

                PreEnrollCourseResponse(
                    preEnrollId = requireNotNull(preEnroll.id),
                    course =
                        CourseDetailResponse(
                            id = requireNotNull(course.id),
                            year = course.year,
                            semester = course.semester,
                            classification = course.classification,
                            college = course.college,
                            department = course.department,
                            academicCourse = course.academicCourse,
                            academicYear = course.academicYear,
                            courseNumber = course.courseNumber,
                            lectureNumber = course.lectureNumber,
                            courseTitle = course.courseTitle,
                            credit = course.credit,
                            instructor = course.instructor,
                            placeAndTime = course.placeAndTime,
                            quota = course.quota,
                            freshmanQuota = course.freshmanQuota,
                        ),
                    cartCount = preEnroll.cartCount,
                )
            }
    }
}
