package com.wafflestudio.team8server.preenroll.repository

import com.wafflestudio.team8server.preenroll.model.PreEnroll
import org.springframework.data.jpa.repository.JpaRepository

interface PreEnrollRepository : JpaRepository<PreEnroll, Long> {
    fun findAllByUserId(userId: Long): List<PreEnroll>

    fun findByUserIdAndCourseId(
        userId: Long,
        courseId: Long,
    ): PreEnroll?

    fun existsByUserIdAndCourseId(
        userId: Long,
        courseId: Long,
    ): Boolean

    fun deleteByUserIdAndCourseId(
        userId: Long,
        courseId: Long,
    ): Long
}
