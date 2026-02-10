package com.wafflestudio.team8server.practice.repository

import com.wafflestudio.team8server.admin.repository.DailyCountProjection
import com.wafflestudio.team8server.practice.model.PracticeDetail
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

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

    @Query(
        value =
            """
            SELECT DATE(pl.practice_at) AS date,
                   COUNT(pd.id)        AS count
            FROM practice_details pd
            INNER JOIN practice_logs pl ON pl.id = pd.log_id
            WHERE pl.practice_at >= :fromAt
              AND pl.practice_at <  :toExclusive
            GROUP BY DATE(pl.practice_at)
            ORDER BY DATE(pl.practice_at) ASC
            """,
        nativeQuery = true,
    )
    fun countDailyPracticeDetails(
        @Param("fromAt") fromAt: LocalDateTime,
        @Param("toExclusive") toExclusive: LocalDateTime,
    ): List<DailyCountProjection>
}
