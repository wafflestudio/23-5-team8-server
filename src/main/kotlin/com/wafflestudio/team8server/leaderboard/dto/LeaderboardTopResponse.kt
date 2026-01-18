package com.wafflestudio.team8server.leaderboard.dto

data class LeaderboardTopResponse(
    val topFirstReactionTime: List<LeaderboardEntryResponse>,
    val topSecondReactionTime: List<LeaderboardEntryResponse>,
    val topCompetitionRate: List<LeaderboardEntryResponse>,
)
