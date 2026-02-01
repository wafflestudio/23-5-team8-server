package com.wafflestudio.team8server.leaderboard.repository

import com.wafflestudio.team8server.leaderboard.model.WeeklyLeaderboardRecord
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface WeeklyLeaderboardRecordRepository : JpaRepository<WeeklyLeaderboardRecord, Long> {
    fun findByUserId(userId: Long): WeeklyLeaderboardRecord?

    @Query(
        """
            SELECT rec
            FROM WeeklyLeaderboardRecord rec
            WHERE rec.bestFirstReactionTime IS NOT NULL
            ORDER BY rec.bestFirstReactionTime ASC, rec.bestFirstReactionTimeAchievedAt ASC
        """,
    )
    fun findTopByBestFirstReactionTime(pageable: Pageable): Page<WeeklyLeaderboardRecord>

    @Query(
        """
            SELECT rec
            FROM WeeklyLeaderboardRecord rec
            WHERE rec.bestSecondReactionTime IS NOT NULL
            ORDER BY rec.bestSecondReactionTime ASC, rec.bestSecondReactionTimeAchievedAt ASC
        """,
    )
    fun findTopByBestSecondReactionTime(pageable: Pageable): Page<WeeklyLeaderboardRecord>

    @Query(
        """
            SELECT rec
            FROM WeeklyLeaderboardRecord rec
            WHERE rec.bestCompetitionRate IS NOT NULL
            ORDER BY rec.bestCompetitionRate DESC, rec.bestCompetitionRateAchievedAt ASC
        """,
    )
    fun findTopByBestCompetitionRate(pageable: Pageable): Page<WeeklyLeaderboardRecord>

    @Query(
        """
        SELECT COUNT(rec)
        FROM WeeklyLeaderboardRecord rec
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
        FROM WeeklyLeaderboardRecord rec
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
        FROM WeeklyLeaderboardRecord rec
        WHERE rec.bestCompetitionRate IS NOT NULL
          AND rec.bestCompetitionRate > :value
    """,
    )
    fun countBetterCompetitionRate(
        @Param("value") value: Double,
    ): Long
}
