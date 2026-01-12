package com.wafflestudio.team8server.course.repository

import com.wafflestudio.team8server.course.model.Course
import org.springframework.data.jpa.repository.JpaRepository

interface CourseRepository : JpaRepository<Course, Long>
