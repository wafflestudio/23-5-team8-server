package com.wafflestudio.team8server.practice.repository

import com.wafflestudio.team8server.practice.model.PracticeLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PracticeLogRepository : JpaRepository<PracticeLog, Long> {
    fun findByUserIdOrderByPracticeAtDesc(
        userId: Long,
        pageable: Pageable,
    ): Page<PracticeLog>

    @Query(
        """
        SELECT pl FROM PracticeLog pl
        WHERE pl.user.id = :userId
        AND EXISTS (SELECT 1 FROM PracticeDetail pd WHERE pd.practiceLog = pl)
        ORDER BY pl.practiceAt DESC
        """,
    )
    fun findByUserIdWithAttemptsOrderByPracticeAtDesc(
        userId: Long,
        pageable: Pageable,
    ): Page<PracticeLog>

    fun findFirstByUserIdOrderByPracticeAtDesc(userId: Long): PracticeLog?

    @Query(
        """
        SELECT pl FROM PracticeLog pl
        WHERE pl.user.id = :userId
        AND EXISTS (SELECT 1 FROM PracticeDetail pd WHERE pd.practiceLog = pl)
        ORDER BY pl.practiceAt DESC
        LIMIT 1
        """,
    )
    fun findFirstByUserIdWithAttemptsOrderByPracticeAtDesc(userId: Long): PracticeLog?
}
