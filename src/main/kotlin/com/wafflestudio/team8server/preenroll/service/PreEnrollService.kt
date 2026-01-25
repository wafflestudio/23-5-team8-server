package com.wafflestudio.team8server.preenroll.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.wafflestudio.team8server.common.exception.DuplicateCourseNumberInPreEnrollException
import com.wafflestudio.team8server.common.exception.PreEnrollAlreadyExistsException
import com.wafflestudio.team8server.common.exception.ResourceNotFoundException
import com.wafflestudio.team8server.common.exception.TimeConflictInPreEnrollException
import com.wafflestudio.team8server.course.dto.CourseDetailResponse
import com.wafflestudio.team8server.course.repository.CourseRepository
import com.wafflestudio.team8server.preenroll.dto.PreEnrollCourseResponse
import com.wafflestudio.team8server.preenroll.model.PreEnroll
import com.wafflestudio.team8server.preenroll.repository.PreEnrollRepository
import com.wafflestudio.team8server.preenroll.util.CourseScheduleUtil
import com.wafflestudio.team8server.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PreEnrollService(
    private val preEnrollRepository: PreEnrollRepository,
    private val userRepository: UserRepository,
    private val courseRepository: CourseRepository,
) {
    private val objectMapper: ObjectMapper = ObjectMapper()

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
            .sortedWith(
                if (overQuotaOnly) {
                    compareByDescending<PreEnroll> { it.cartCount.toDouble() / it.course.quota.toDouble() }
                        .thenBy { it.course.courseNumber }
                        .thenBy { it.course.lectureNumber }
                } else {
                    compareBy({ it.course.courseNumber }, { it.course.lectureNumber })
                },
            ).map { preEnroll ->
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

    @Transactional
    fun addPreEnroll(
        userId: Long,
        courseId: Long,
    ): PreEnrollCourseResponse {
        val user =
            userRepository
                .findById(userId)
                .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다") }

        val course =
            courseRepository
                .findById(courseId)
                .orElseThrow { ResourceNotFoundException("강의를 찾을 수 없습니다") }

        if (preEnrollRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw PreEnrollAlreadyExistsException()
        }

        val existing = preEnrollRepository.findAllByUserId(userId)

        if (existing.any { it.course.courseNumber == course.courseNumber }) {
            throw DuplicateCourseNumberInPreEnrollException()
        }

        for (item in existing) {
            val conflict =
                CourseScheduleUtil.hasTimeConflict(
                    objectMapper = objectMapper,
                    placeAndTimeJsonA = item.course.placeAndTime,
                    placeAndTimeJsonB = course.placeAndTime,
                )
            if (conflict) {
                throw TimeConflictInPreEnrollException()
            }
        }

        val saved =
            preEnrollRepository.save(
                PreEnroll(
                    user = user,
                    course = course,
                    cartCount = 0,
                ),
            )

        return PreEnrollCourseResponse(
            preEnrollId = requireNotNull(saved.id),
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
            cartCount = saved.cartCount,
        )
    }

    @Transactional
    fun deletePreEnroll(
        userId: Long,
        courseId: Long,
    ) {
        val preEnroll =
            preEnrollRepository.findByUserIdAndCourseId(userId, courseId)
                ?: throw ResourceNotFoundException("장바구니에서 해당 강의를 찾을 수 없습니다")

        preEnrollRepository.delete(preEnroll)
    }

    @Transactional
    fun updateCartCount(
        userId: Long,
        courseId: Long,
        cartCount: Int,
    ): PreEnrollCourseResponse {
        val preEnroll =
            preEnrollRepository.findByUserIdAndCourseId(userId, courseId)
                ?: throw ResourceNotFoundException("장바구니에서 해당 강의를 찾을 수 없습니다")

        preEnroll.cartCount = cartCount

        val saved = preEnrollRepository.save(preEnroll)
        val course = saved.course

        return PreEnrollCourseResponse(
            preEnrollId = requireNotNull(saved.id),
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
            cartCount = saved.cartCount,
        )
    }
}
