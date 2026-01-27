package com.wafflestudio.team8server.course.repository

import com.wafflestudio.team8server.course.model.Course
import jakarta.persistence.criteria.Predicate
import org.springframework.data.jpa.domain.Specification

object CourseSpecification {
    fun search(
        query: String?,
        courseNumber: String?,
        academicCourse: String?,
        academicYear: Int?,
        college: String?,
        department: String?,
        classification: String?,
    ): Specification<Course> =
        Specification { root, _, cb ->
            val predicates = mutableListOf<Predicate>()

            if (!query.isNullOrBlank()) {
                val trimmedQuery = query.trim()
                val likeQuery = "%$trimmedQuery%"

                val noSpaceQuery = trimmedQuery.replace(" ", "")
                val likeNoSpaceQuery = "%$noSpaceQuery%"

                val titlePath = root.get<String>("courseTitle")
                val instructorPath = root.get<String>("instructor")

                val orPredicates =
                    mutableListOf(
                        cb.like(titlePath, likeQuery),
                        cb.like(instructorPath, likeQuery),
                    )

                if (noSpaceQuery.isNotBlank()) {
                    val courseTitleNoSpace =
                        cb.function(
                            "replace",
                            String::class.java,
                            titlePath,
                            cb.literal(" "),
                            cb.literal(""),
                        )

                    val instructorNoSpace =
                        cb.function(
                            "replace",
                            String::class.java,
                            instructorPath,
                            cb.literal(" "),
                            cb.literal(""),
                        )

                    orPredicates += cb.like(courseTitleNoSpace, likeNoSpaceQuery)
                    orPredicates += cb.like(instructorNoSpace, likeNoSpaceQuery)
                }

                predicates += cb.or(*orPredicates.toTypedArray())
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
