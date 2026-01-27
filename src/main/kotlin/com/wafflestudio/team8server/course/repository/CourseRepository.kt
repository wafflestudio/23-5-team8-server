package com.wafflestudio.team8server.course.repository

import com.wafflestudio.team8server.course.model.Course
import com.wafflestudio.team8server.course.model.Semester
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface CourseRepository :
    JpaRepository<Course, Long>,
    JpaSpecificationExecutor<Course> {
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        """
        delete from Course c
        where c.year = :year
          and c.semester = :semester
        """,
    )
    fun deleteAllByYearAndSemester(
        year: Int,
        semester: Semester,
    ): Long
}
