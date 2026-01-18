package com.wafflestudio.team8server.leaderboard.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "로그인한 유저의 리더보드 등재 기록")
data class MyLeaderboardResponse(
    @Schema(description = "1픽(첫번째 수강신청) 최단 반응시간(ms)", example = "200", nullable = true)
    val bestFirstReactionTime: Int?,
    @Schema(description = "2픽(두번째 수강신청) 최단 반응시간(ms)", example = "400", nullable = true)
    val bestSecondReactionTime: Int?,
    @Schema(description = "성공시켜 본 최고 경쟁률", example = "2.5", nullable = true)
    val bestCompetitionRate: Double?,
)
