package com.wafflestudio.team8server.leaderboard.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "기준별 리더보드 페이지네이션 응답")
data class LeaderboardTopPagedResponse(
    @Schema(description = "1픽(첫번째 수강신청) 최단 반응시간(ms)")
    val topFirstReactionTime: LeaderboardPagedSectionResponse,
    @Schema(description = "2픽(두번째 수강신청) 최단 반응시간(ms)")
    val topSecondReactionTime: LeaderboardPagedSectionResponse,
    @Schema(description = "성공시켜 본 최고 경쟁률")
    val topCompetitionRate: LeaderboardPagedSectionResponse,
)
