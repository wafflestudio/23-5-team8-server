package com.wafflestudio.team8server.course.repository

import com.wafflestudio.team8server.course.model.Course
import jakarta.persistence.criteria.Predicate
import org.springframework.data.jpa.domain.Specification

object CourseSpecification {
    fun search(
        courseTitle: String?,
        courseNumber: String?,
        academicCourse: String?,
        academicYear: Int?,
        college: String?,
        department: String?,
        classification: String?,
    ): Specification<Course> =
        Specification { root, _, cb ->
            val predicates = mutableListOf<Predicate>()

            if (!courseTitle.isNullOrBlank()) {
                predicates +=
                    cb.like(
                        root.get("courseTitle"),
                        "%${courseTitle.trim()}%",
                    )
            }

            if (!courseNumber.isNullOrBlank()) {
                predicates +=
                    cb.equal(
                        root.get<String>("courseNumber"),
                        courseNumber.trim(),
                    )
            }

            if (!academicCourse.isNullOrBlank()) {
                predicates += cb.equal(root.get<String>("academicCourse"), academicCourse)
            }

            if (academicYear != null) {
                predicates += cb.equal(root.get<Int>("academicYear"), academicYear)
            }

            if (!college.isNullOrBlank()) {
                predicates += cb.equal(root.get<String>("college"), college)
            }

            if (!department.isNullOrBlank()) {
                predicates += cb.equal(root.get<String>("department"), department)
            }

            if (!classification.isNullOrBlank()) {
                predicates += cb.equal(root.get<String>("classification"), classification)
            }

            cb.and(*predicates.toTypedArray())
        }
}
