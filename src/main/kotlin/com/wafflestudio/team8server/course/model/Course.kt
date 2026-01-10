package com.wafflestudio.team8server.course.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 개설 강의 정보 엔티티
 */
@Entity
@Table(name = "courses")
class Course(
    @Column(name = "year", nullable = false)
    val year: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "semester", nullable = false)
    val semester: Semester,

    @Column(name = "classification", length = 50)
    val classification: String? = null,

    @Column(name = "college", length = 100)
    val college: String? = null,

    @Column(name = "department", length = 100)
    val department: String? = null,

    @Column(name = "academic_course", length = 50)
    val academicCourse: String? = null,

    @Column(name = "academic_year", length = 20)
    val academicYear: String? = null,

    @Column(name = "course_number", length = 20, nullable = false)
    val courseNumber: String,

    @Column(name = "lecture_number", length = 10, nullable = false)
    val lectureNumber: String,

    @Column(name = "course_title", nullable = false)
    val courseTitle: String,

    @Column(name = "credit")
    val credit: Int? = null,

    @Column(name = "instructor", length = 100)
    val instructor: String? = null,

    @Column(name = "place_and_time", columnDefinition = "JSON")
    val placeAndTime: String? = null,

    @Column(name = "quota", nullable = false)
    val quota: Int,

    @Column(name = "freshman_quota")
    val freshmanQuota: Int? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)

enum class Semester {
    SPRING,
    SUMMER,
    FALL,
    WINTER,
}
