package com.wafflestudio.team8server.leaderboard.repository

import com.wafflestudio.team8server.leaderboard.model.LeaderboardRecord
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface LeaderboardRecordRepository : JpaRepository<LeaderboardRecord, Long> {
    fun findByUserId(userId: Long): LeaderboardRecord?

    @Query(
        """
            SELECT rec
            FROM LeaderboardRecord rec
            WHERE rec.bestFirstReactionTime IS NOT NULL
            ORDER BY rec.bestFirstReactionTime ASC
        """,
    )
    fun findTopByBestFirstReactionTime(pageable: Pageable): List<LeaderboardRecord>

    @Query(
        """
            SELECT rec
            FROM LeaderboardRecord rec
            WHERE rec.bestSecondReactionTime IS NOT NULL
            ORDER BY rec.bestSecondReactionTime ASC
        """,
    )
    fun findTopByBestSecondReactionTime(pageable: Pageable): List<LeaderboardRecord>

    @Query(
        """
            SELECT rec
            FROM LeaderboardRecord rec
            WHERE rec.bestCompetitionRate IS NOT NULL
            ORDER BY rec.bestCompetitionRate DESC
        """,
    )
    fun findTopByBestCompetitionRate(pageable: Pageable): List<LeaderboardRecord>

    @Query(
        """
        SELECT COUNT(rec)
        FROM LeaderboardRecord rec
        WHERE rec.bestFirstReactionTime IS NOT NULL
          AND rec.bestFirstReactionTime < :value
    """,
    )
    fun countBetterFirstReactionTime(
        @Param("value") value: Int,
    ): Long

    @Query(
        """
        SELECT COUNT(rec)
        FROM LeaderboardRecord rec
        WHERE rec.bestSecondReactionTime IS NOT NULL
          AND rec.bestSecondReactionTime < :value
    """,
    )
    fun countBetterSecondReactionTime(
        @Param("value") value: Int,
    ): Long

    @Query(
        """
        SELECT COUNT(rec)
        FROM LeaderboardRecord rec
        WHERE rec.bestCompetitionRate IS NOT NULL
          AND rec.bestCompetitionRate > :value
    """,
    )
    fun countBetterCompetitionRate(
        @Param("value") value: Double,
    ): Long
}
