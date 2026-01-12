package com.wafflestudio.team8server.course.repository

import com.wafflestudio.team8server.course.model.Course
import com.wafflestudio.team8server.course.model.Semester
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface CourseRepository :
    JpaRepository<Course, Long>,
    JpaSpecificationExecutor<Course> {
    fun deleteAllByYearAndSemester(
        year: Int,
        semester: Semester,
    ): Long
}
