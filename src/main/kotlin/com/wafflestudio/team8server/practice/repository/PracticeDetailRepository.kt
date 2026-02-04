package com.wafflestudio.team8server.practice.repository

import com.wafflestudio.team8server.practice.model.PracticeDetail
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PracticeDetailRepository : JpaRepository<PracticeDetail, Long> {
    @Query("SELECT pd.reactionTime FROM PracticeDetail pd ORDER BY pd.reactionTime ASC")
    fun findAllReactionTimesOrderByAsc(): List<Int>

    fun countByPracticeLogId(practiceLogId: Long): Long

    fun findByPracticeLogIdOrderByReactionTimeAsc(practiceLogId: Long): List<PracticeDetail>

    fun findByPracticeLogIdAndCourseId(
        practiceLogId: Long,
        courseId: Long,
    ): PracticeDetail?

    fun countByPracticeLogIdAndIsSuccess(
        practiceLogId: Long,
        isSuccess: Boolean,
    ): Long

    fun findByPracticeLogIdOrderByIdAsc(practiceLogId: Long): List<PracticeDetail>
}
