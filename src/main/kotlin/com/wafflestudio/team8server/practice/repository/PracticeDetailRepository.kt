package com.wafflestudio.team8server.practice.repository

import com.wafflestudio.team8server.practice.model.PracticeDetail
import org.springframework.data.jpa.repository.JpaRepository

interface PracticeDetailRepository : JpaRepository<PracticeDetail, Long> {
    fun countByPracticeLogId(practiceLogId: Long): Long

    fun findByPracticeLogId(practiceLogId: Long): List<PracticeDetail>

    fun findByPracticeLogIdAndCourseId(
        practiceLogId: Long,
        courseId: Long,
    ): PracticeDetail?
}
