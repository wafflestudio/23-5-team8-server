package com.wafflestudio.team8server.leaderboard.controller

import com.wafflestudio.team8server.common.auth.LoggedInUserId
import com.wafflestudio.team8server.common.exception.ErrorResponse
import com.wafflestudio.team8server.common.exception.ResourceNotFoundException
import com.wafflestudio.team8server.leaderboard.dto.LeaderboardEntryResponse
import com.wafflestudio.team8server.leaderboard.dto.LeaderboardTopResponse
import com.wafflestudio.team8server.leaderboard.dto.MyLeaderboardResponse
import com.wafflestudio.team8server.leaderboard.service.LeaderboardService
import com.wafflestudio.team8server.user.repository.UserRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "리더보드 API", description = "수강신청 연습 기록을 바탕으로 3가지 기준의 리더보드를 제공합니다.")
@RestController
@RequestMapping("/api/leaderboard")
class LeaderboardController(
    private val leaderboardService: LeaderboardService,
    private val userRepository: UserRepository,
) {
    @Operation(
        summary = "상위 n명 리더보드 조회",
        description = """
            저장된 리더보드 기록 중 3가지 기준의 상위 n명을 반환합니다.
            
            기준:
            - topFirstReactionTime: 1픽(첫번째 수강신청) 최단 반응시간(ms)
            - topSecondReactionTime: 2픽(두번째 수강신청) 최단 반응시간(ms)
            - topCompetitionRate: 성공시켜 본 최고 경쟁률(total_competitors / capacity)
            
            limit: 기본값 10, 범위 1~100
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "상위 n명 리더보드 조회 성공",
                content = [
                    Content(
                        schema = Schema(implementation = LeaderboardTopResponse::class),
                        examples = [
                            ExampleObject(
                                name = "leaderboard-top-success",
                                summary = "상위 n명 응답 예시",
                                value = """
                                {
                                  "topFirstReactionTime": [
                                    { "userId": 1, "nickname": "user1", "profileImageUrl": null, "value": 150.0 },
                                    { "userId": 2, "nickname": "user2", "profileImageUrl": null, "value": 180.0 }
                                  ],
                                  "topSecondReactionTime": [
                                    { "userId": 2, "nickname": "user2", "profileImageUrl": null, "value": 400.0 }
                                  ],
                                  "topCompetitionRate": [
                                    { "userId": 3, "nickname": "user3", "profileImageUrl": null, "value": 4.8 }
                                  ]
                                }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    @GetMapping
    fun getTop(
        @Parameter(
            description = "기준별 상위 N명을 조회합니다 (default: 10, max: 100)",
            example = "10",
        )
        @RequestParam(required = false, defaultValue = "10")
        limit: Int,
    ): LeaderboardTopResponse {
        val result = leaderboardService.getTopResult(limit)

        return LeaderboardTopResponse(
            topFirstReactionTime =
                result.topFirstReactionTime.mapNotNull { record ->
                    val user =
                        userRepository
                            .findById(record.userId)
                            .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다") }
                    val value = record.bestFirstReactionTime ?: return@mapNotNull null
                    LeaderboardEntryResponse(
                        userId = user.id ?: return@mapNotNull null,
                        nickname = user.nickname,
                        profileImageUrl = user.profileImageUrl,
                        value = value.toDouble(),
                    )
                },
            topSecondReactionTime =
                result.topSecondReactionTime.mapNotNull { record ->
                    val user =
                        userRepository
                            .findById(record.userId)
                            .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다") }
                    val value = record.bestSecondReactionTime ?: return@mapNotNull null
                    LeaderboardEntryResponse(
                        userId = user.id ?: return@mapNotNull null,
                        nickname = user.nickname,
                        profileImageUrl = user.profileImageUrl,
                        value = value.toDouble(),
                    )
                },
            topCompetitionRate =
                result.topCompetitionRate.mapNotNull { record ->
                    val user =
                        userRepository
                            .findById(record.userId)
                            .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다") }
                    val value = record.bestCompetitionRate ?: return@mapNotNull null
                    LeaderboardEntryResponse(
                        userId = user.id ?: return@mapNotNull null,
                        nickname = user.nickname,
                        profileImageUrl = user.profileImageUrl,
                        value = value,
                    )
                },
        )
    }

    @Operation(
        summary = "로그인한 유저의 리더보드 최고 기록 조회",
        description = "로그인한 유저의 3가지 기준 최고 기록을 반환합니다.",
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "로그인한 유저의 최고 기록 조회 성공",
                content = [
                    Content(
                        schema = Schema(implementation = MyLeaderboardResponse::class),
                        examples = [
                            ExampleObject(
                                name = "my-leaderboard-success",
                                summary = "로그인한 유저의 기록 응답 예시",
                                value = """
                                {
                                  "bestFirstReactionTime": 240,
                                  "bestSecondReactionTime": 550,
                                  "bestCompetitionRate": 3.2
                                }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "unauthorized",
                                summary = "인증 실패",
                                value = """
                                {
                                    "timestamp": "2026-01-19T02:00:00",
                                    "status": 401,
                                    "error": "Unauthorized",
                                    "message": "인증에 실패했습니다",
                                    "errorCode": "UNAUTHORIZED",
                                    "validationErrors": null
                                }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    @GetMapping("/me")
    fun getMy(
        @Parameter(hidden = true)
        @LoggedInUserId userId: Long,
    ): MyLeaderboardResponse {
        val result = leaderboardService.getMyResult(userId)

        return MyLeaderboardResponse(
            bestFirstReactionTime = result.bestFirstReactionTime,
            bestFirstReactionTimeRank = result.bestFirstReactionTimeRank,
            bestSecondReactionTime = result.bestSecondReactionTime,
            bestSecondReactionTimeRank = result.bestSecondReactionTimeRank,
            bestCompetitionRate = result.bestCompetitionRate,
            bestCompetitionRateRank = result.bestCompetitionRateRank,
        )
    }
}
