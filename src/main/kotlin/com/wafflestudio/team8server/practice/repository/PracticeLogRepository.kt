package com.wafflestudio.team8server.practice.repository

import com.wafflestudio.team8server.admin.repository.DailyCountProjection
import com.wafflestudio.team8server.practice.model.PracticeLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface PracticeLogRepository : JpaRepository<PracticeLog, Long> {
    fun findByUserIdOrderByPracticeAtDescIdDesc(
        userId: Long,
        pageable: Pageable,
    ): Page<PracticeLog>

    @Query(
        """
        SELECT pl FROM PracticeLog pl
        WHERE pl.user.id = :userId
        AND EXISTS (SELECT 1 FROM PracticeDetail pd WHERE pd.practiceLog = pl)
        ORDER BY pl.practiceAt DESC, pl.id DESC
        """,
    )
    fun findByUserIdWithAttemptsOrderByPracticeAtDesc(
        userId: Long,
        pageable: Pageable,
    ): Page<PracticeLog>

    fun findFirstByUserIdOrderByPracticeAtDescIdDesc(userId: Long): PracticeLog?

    @Query(
        """
        SELECT pl FROM PracticeLog pl
        WHERE pl.user.id = :userId
        AND EXISTS (SELECT 1 FROM PracticeDetail pd WHERE pd.practiceLog = pl)
        ORDER BY pl.practiceAt DESC, pl.id DESC
        LIMIT 1
        """,
    )
    fun findFirstByUserIdWithAttemptsOrderByPracticeAtDesc(userId: Long): PracticeLog?

    @Query(
        value =
            """
            SELECT DATE(pl.practice_at)        AS date,
                   COUNT(DISTINCT pl.user_id) AS count
            FROM practice_logs pl
            INNER JOIN practice_details pd ON pd.log_id = pl.id
            WHERE pl.practice_at >= :fromAt
              AND pl.practice_at <  :toExclusive
            GROUP BY DATE(pl.practice_at)
            ORDER BY DATE(pl.practice_at) ASC
            """,
        nativeQuery = true,
    )
    fun countDailyActiveUsers(
        @Param("fromAt") fromAt: LocalDateTime,
        @Param("toExclusive") toExclusive: LocalDateTime,
    ): List<DailyCountProjection>
}
