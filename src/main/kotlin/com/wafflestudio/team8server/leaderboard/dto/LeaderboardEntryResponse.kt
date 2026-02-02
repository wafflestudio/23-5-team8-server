package com.wafflestudio.team8server.leaderboard.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "리더보드 항목(유저 + 특정 기준의 value)",
)
data class LeaderboardEntryResponse(
    @Schema(description = "유저 ID", example = "1")
    val userId: Long,
    @Schema(description = "유저 닉네임", example = "닉네임")
    val nickname: String,
    @Schema(description = "프로필 이미지 URL", example = "https://example.com/image.png", nullable = true)
    val profileImageUrl: String?,
    @Schema(
        description = """
            특정 기준의 value
            - reaction time: ms 단위
            - competition rate: 총 경쟁자 수 / 정원
        """,
        example = "200.0",
    )
    val value: Double,
    @Schema(
        description = "순위 (동일 기록은 같은 순위, 다음 순위는 중복된 만큼 밀림. 예: 1, 1, 3, 4)",
        example = "1",
    )
    val rank: Long,
)
