package com.wafflestudio.team8server.leaderboard.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "weekly_leaderboard_records")
class WeeklyLeaderboardRecord(
    @Id
    @Column(name = "user_id")
    val userId: Long,
    @Column(name = "best_first_reaction_time")
    var bestFirstReactionTime: Int? = null,
    @Column(name = "best_second_reaction_time")
    var bestSecondReactionTime: Int? = null,
    @Column(name = "best_competition_rate")
    var bestCompetitionRate: Double? = null,
)
