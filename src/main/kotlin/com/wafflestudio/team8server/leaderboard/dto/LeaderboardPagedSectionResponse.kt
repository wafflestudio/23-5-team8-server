package com.wafflestudio.team8server.leaderboard.dto

import com.wafflestudio.team8server.common.dto.PageInfo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "리더보드 기준별 페이지네이션 응답")
data class LeaderboardPagedSectionResponse(
    @Schema(description = "리더보드 항목 목록")
    val items: List<LeaderboardEntryResponse>,
    @Schema(description = "pagination 정보")
    val pageInfo: PageInfo,
)
