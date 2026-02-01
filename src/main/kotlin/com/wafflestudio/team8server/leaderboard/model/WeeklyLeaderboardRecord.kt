package com.wafflestudio.team8server.leaderboard.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "weekly_leaderboard_records")
class WeeklyLeaderboardRecord(
    @Id
    @Column(name = "user_id")
    val userId: Long,
    @Column(name = "best_first_reaction_time")
    var bestFirstReactionTime: Int? = null,
    @Column(name = "best_first_reaction_time_achieved_at")
    var bestFirstReactionTimeAchievedAt: Instant? = null,
    @Column(name = "best_second_reaction_time")
    var bestSecondReactionTime: Int? = null,
    @Column(name = "best_second_reaction_time_achieved_at")
    var bestSecondReactionTimeAchievedAt: Instant? = null,
    @Column(name = "best_competition_rate")
    var bestCompetitionRate: Double? = null,
    @Column(name = "best_competition_rate_achieved_at")
    var bestCompetitionRateAchievedAt: Instant? = null,
)
