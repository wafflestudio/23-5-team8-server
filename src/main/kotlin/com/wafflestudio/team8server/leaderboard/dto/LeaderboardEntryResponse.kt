package com.wafflestudio.team8server.leaderboard.dto

data class LeaderboardEntryResponse(
    val userId: Long,
    val nickname: String,
    val profileImageUrl: String?,
    val value: Double,
)
