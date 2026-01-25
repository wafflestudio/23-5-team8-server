package com.wafflestudio.team8server.leaderboard.controller

import com.wafflestudio.team8server.common.auth.LoggedInUserId
import com.wafflestudio.team8server.common.dto.PageInfo
import com.wafflestudio.team8server.common.exception.ErrorResponse
import com.wafflestudio.team8server.common.exception.ResourceNotFoundException
import com.wafflestudio.team8server.leaderboard.dto.LeaderboardEntryResponse
import com.wafflestudio.team8server.leaderboard.dto.LeaderboardPageRequest
import com.wafflestudio.team8server.leaderboard.dto.LeaderboardPagedSectionResponse
import com.wafflestudio.team8server.leaderboard.dto.LeaderboardTopPagedResponse
import com.wafflestudio.team8server.leaderboard.dto.MyLeaderboardResponse
import com.wafflestudio.team8server.leaderboard.service.LeaderboardService
import com.wafflestudio.team8server.user.model.User
import com.wafflestudio.team8server.user.repository.UserRepository
import com.wafflestudio.team8server.user.service.ProfileImageUrlResolver
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "리더보드 API", description = "수강신청 연습 기록을 바탕으로 3가지 기준의 리더보드를 제공합니다.")
@RestController
@RequestMapping("/api/leaderboard")
class LeaderboardController(
    private val leaderboardService: LeaderboardService,
    private val userRepository: UserRepository,
    private val profileImageUrlResolver: ProfileImageUrlResolver,
) {
    @Operation(
        summary = "리더보드 조회 (페이지네이션 적용)",
        description = """
            저장된 리더보드 기록 중 3가지 기준의 순위 기록을 페이지네이션하여 반환합니다.
            
            기준:
            - topFirstReactionTime: 1픽(첫번째 수강신청) 최단 반응시간(ms)
            - topSecondReactionTime: 2픽(두번째 수강신청) 최단 반응시간(ms)
            - topCompetitionRate: 성공시켜 본 최고 경쟁률(total_competitors / capacity)
            
            - page는 0부터 시작합니다.
            - size의 범위는 1-100입니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "리더보드 조회 성공",
                content = [
                    Content(
                        schema = Schema(implementation = LeaderboardTopPagedResponse::class),
                        examples = [
                            ExampleObject(
                                name = "leaderboard-top-paged-success",
                                summary = "페이지네이션 응답 예시",
                                value = """
                                {
                                  "topFirstReactionTime": {
                                    "items": [
                                      { "userId": 1, "nickname": "user1", "profileImageUrl": null, "value": 150.0 }
                                    ],
                                    "pageInfo": { "page": 0, "size": 10, "totalElements": 123, "totalPages": 13, "hasNext": true }
                                  },
                                  "topSecondReactionTime": {
                                    "items": [],
                                    "pageInfo": { "page": 0, "size": 10, "totalElements": 0, "totalPages": 0, "hasNext": false }
                                  },
                                  "topCompetitionRate": {
                                    "items": [
                                      { "userId": 3, "nickname": "user3", "profileImageUrl": null, "value": 4.8 }
                                    ],
                                    "pageInfo": { "page": 0, "size": 10, "totalElements": 55, "totalPages": 6, "hasNext": true }
                                  }
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
        @ParameterObject request: LeaderboardPageRequest,
    ): LeaderboardTopPagedResponse {
        val result = leaderboardService.getTopResult(page = request.page, size = request.size)

        val firstSection =
            toSectionResponse(
                page = result.topFirstReactionTime,
                valueSelector = { it.bestFirstReactionTime?.toDouble() },
            )

        val secondSection =
            toSectionResponse(
                page = result.topSecondReactionTime,
                valueSelector = { it.bestSecondReactionTime?.toDouble() },
            )

        val rateSection =
            toSectionResponse(
                page = result.topCompetitionRate,
                valueSelector = { it.bestCompetitionRate },
            )

        return LeaderboardTopPagedResponse(
            topFirstReactionTime = firstSection,
            topSecondReactionTime = secondSection,
            topCompetitionRate = rateSection,
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
                                  "bestFirstReactionTimeRank": 85
                                  "bestSecondReactionTime": 550,
                                  "bestSecondReactionTimeRank": 42
                                  "bestCompetitionRate": 3.2
                                  "bestCompetitionRateRank": 14
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

    @Operation(
        summary = "주간 리더보드 조회 (페이지네이션 적용)",
        description = """
            주간 리더보드는 매주 월요일 00:00(Asia/Seoul)에 초기화됩니다.
            이외 API 세부 사항은 전체 리더보드 조회와 동일합니다.
        """,
    )
    @GetMapping("/weekly")
    fun getWeeklyTop(
        @ParameterObject request: LeaderboardPageRequest,
    ): LeaderboardTopPagedResponse {
        val result = leaderboardService.getWeeklyTopResult(page = request.page, size = request.size)

        val firstSection =
            toWeeklySectionResponse(
                page = result.topFirstReactionTime,
                valueSelector = { it.bestFirstReactionTime?.toDouble() },
            )

        val secondSection =
            toWeeklySectionResponse(
                page = result.topSecondReactionTime,
                valueSelector = { it.bestSecondReactionTime?.toDouble() },
            )

        val rateSection =
            toWeeklySectionResponse(
                page = result.topCompetitionRate,
                valueSelector = { it.bestCompetitionRate },
            )

        return LeaderboardTopPagedResponse(
            topFirstReactionTime = firstSection,
            topSecondReactionTime = secondSection,
            topCompetitionRate = rateSection,
        )
    }

    @Operation(
        summary = "로그인한 유저의 주간 리더보드 최고 기록 조회",
        description = """
            주간 리더보드는 매주 월요일 00:00(Asia/Seoul)에 초기화됩니다.
            이외 API 세부 사항은 전체 리더보드 조회와 동일합니다.
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    @GetMapping("/weekly/me")
    fun getWeeklyMy(
        @Parameter(hidden = true)
        @LoggedInUserId userId: Long,
    ): MyLeaderboardResponse {
        val result = leaderboardService.getWeeklyMyResult(userId)

        return MyLeaderboardResponse(
            bestFirstReactionTime = result.bestFirstReactionTime,
            bestFirstReactionTimeRank = result.bestFirstReactionTimeRank,
            bestSecondReactionTime = result.bestSecondReactionTime,
            bestSecondReactionTimeRank = result.bestSecondReactionTimeRank,
            bestCompetitionRate = result.bestCompetitionRate,
            bestCompetitionRateRank = result.bestCompetitionRateRank,
        )
    }

    private fun toSectionResponse(
        page: Page<com.wafflestudio.team8server.leaderboard.model.LeaderboardRecord>,
        valueSelector: (com.wafflestudio.team8server.leaderboard.model.LeaderboardRecord) -> Double?,
    ): LeaderboardPagedSectionResponse {
        val usersById = loadUsersById(page.content.map { it.userId })

        val items =
            page.content.mapNotNull { record ->
                val value = valueSelector(record) ?: return@mapNotNull null
                val user = usersById[record.userId] ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다")
                LeaderboardEntryResponse(
                    userId = user.id ?: return@mapNotNull null,
                    nickname = user.nickname,
                    profileImageUrl = user.profileImageUrl,
                    value = value,
                )
            }

        return LeaderboardPagedSectionResponse(
            items = items,
            pageInfo =
                PageInfo(
                    page = page.number,
                    size = page.size,
                    totalElements = page.totalElements,
                    totalPages = page.totalPages,
                    hasNext = page.hasNext(),
                ),
        )
    }

    private fun toWeeklySectionResponse(
        page: Page<com.wafflestudio.team8server.leaderboard.model.WeeklyLeaderboardRecord>,
        valueSelector: (com.wafflestudio.team8server.leaderboard.model.WeeklyLeaderboardRecord) -> Double?,
    ): LeaderboardPagedSectionResponse {
        val usersById = loadUsersById(page.content.map { it.userId })

        val items =
            page.content.mapNotNull { record ->
                val value = valueSelector(record) ?: return@mapNotNull null
                val user = usersById[record.userId] ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다")
                LeaderboardEntryResponse(
                    userId = user.id ?: return@mapNotNull null,
                    nickname = user.nickname,
                    profileImageUrl = user.profileImageUrl,
                    value = value,
                )
            }

        return LeaderboardPagedSectionResponse(
            items = items,
            pageInfo =
                PageInfo(
                    page = page.number,
                    size = page.size,
                    totalElements = page.totalElements,
                    totalPages = page.totalPages,
                    hasNext = page.hasNext(),
                ),
        )
    }

    private fun loadUsersById(userIds: List<Long>): Map<Long, User> {
        if (userIds.isEmpty()) {
            return emptyMap()
        }

        val users = userRepository.findAllById(userIds)
        val map = users.associateBy { it.id ?: 0L }

        for (id in userIds.distinct()) {
            if (!map.containsKey(id)) {
                throw ResourceNotFoundException("사용자를 찾을 수 없습니다")
            }
        }

        return map
    }
}
