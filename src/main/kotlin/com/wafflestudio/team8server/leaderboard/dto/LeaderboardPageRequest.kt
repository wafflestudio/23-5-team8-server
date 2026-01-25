package com.wafflestudio.team8server.leaderboard.dto

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema

data class LeaderboardPageRequest(
    @Parameter(
        description = "페이지 (0부터 시작)",
        example = "0",
        required = true,
    )
    @Schema(nullable = false)
    val page: Int = 0,
    @Parameter(
        description = "페이지 크기 (1-100)",
        example = "10",
        required = true,
    )
    @Schema(nullable = false)
    val size: Int = 10,
)
