package com.wafflestudio.team8server.leaderboard

import com.fasterxml.jackson.databind.ObjectMapper
import com.wafflestudio.team8server.TestcontainersConfiguration
import com.wafflestudio.team8server.leaderboard.model.LeaderboardRecord
import com.wafflestudio.team8server.leaderboard.model.WeeklyLeaderboardRecord
import com.wafflestudio.team8server.leaderboard.repository.LeaderboardRecordRepository
import com.wafflestudio.team8server.leaderboard.repository.WeeklyLeaderboardRecordRepository
import com.wafflestudio.team8server.user.JwtTokenProvider
import com.wafflestudio.team8server.user.model.User
import com.wafflestudio.team8server.user.model.UserRole
import com.wafflestudio.team8server.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class LeaderboardControllerTest
    @Autowired
    constructor(
        private val webApplicationContext: WebApplicationContext,
        private val userRepository: UserRepository,
        private val leaderboardRecordRepository: LeaderboardRecordRepository,
        private val weeklyLeaderboardRecordRepository: WeeklyLeaderboardRecordRepository,
        private val jwtTokenProvider: JwtTokenProvider,
    ) {
        private lateinit var mockMvc: MockMvc
        private val objectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules()

        private lateinit var user1: User
        private lateinit var user2: User
        private lateinit var user3: User
        private lateinit var token1: String

        @BeforeEach
        fun setUp() {
            mockMvc =
                MockMvcBuilders
                    .webAppContextSetup(webApplicationContext)
                    .apply<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(springSecurity())
                    .build()

            leaderboardRecordRepository.deleteAll()
            weeklyLeaderboardRecordRepository.deleteAll()
            userRepository.deleteAll()

            user1 =
                userRepository.save(
                    User(nickname = "유저1", role = UserRole.USER),
                )
            user2 =
                userRepository.save(
                    User(nickname = "유저2", role = UserRole.USER),
                )
            user3 =
                userRepository.save(
                    User(nickname = "유저3", role = UserRole.USER),
                )
            token1 = jwtTokenProvider.createToken(user1.id, "USER")
        }

        @Nested
        @DisplayName("전체 리더보드 조회 (GET /api/leaderboard)")
        inner class GetLeaderboard {
            @Test
            @DisplayName("데이터가 없을 때 빈 리더보드 반환")
            fun `get leaderboard with no data returns empty sections`() {
                mockMvc
                    .perform(get("/api/leaderboard"))
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.topFirstReactionTime.items").isArray)
                    .andExpect(jsonPath("$.topFirstReactionTime.items.length()").value(0))
                    .andExpect(jsonPath("$.topSecondReactionTime.items").isArray)
                    .andExpect(jsonPath("$.topSecondReactionTime.items.length()").value(0))
                    .andExpect(jsonPath("$.topCompetitionRate.items").isArray)
                    .andExpect(jsonPath("$.topCompetitionRate.items.length()").value(0))
            }

            @Test
            @DisplayName("1픽 반응시간 기준 정렬 (오름차순)")
            fun `get leaderboard sorted by first reaction time ascending`() {
                val now = Instant.now()
                leaderboardRecordRepository.saveAll(
                    listOf(
                        LeaderboardRecord(
                            userId = user1.id!!,
                            bestFirstReactionTime = 200,
                            bestFirstReactionTimeAchievedAt = now,
                        ),
                        LeaderboardRecord(
                            userId = user2.id!!,
                            bestFirstReactionTime = 100,
                            bestFirstReactionTimeAchievedAt = now,
                        ),
                        LeaderboardRecord(
                            userId = user3.id!!,
                            bestFirstReactionTime = 300,
                            bestFirstReactionTimeAchievedAt = now,
                        ),
                    ),
                )

                mockMvc
                    .perform(get("/api/leaderboard"))
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.topFirstReactionTime.items.length()").value(3))
                    .andExpect(jsonPath("$.topFirstReactionTime.items[0].value").value(100.0))
                    .andExpect(jsonPath("$.topFirstReactionTime.items[0].rank").value(1))
                    .andExpect(jsonPath("$.topFirstReactionTime.items[0].nickname").value("유저2"))
                    .andExpect(jsonPath("$.topFirstReactionTime.items[1].value").value(200.0))
                    .andExpect(jsonPath("$.topFirstReactionTime.items[1].rank").value(2))
                    .andExpect(jsonPath("$.topFirstReactionTime.items[2].value").value(300.0))
                    .andExpect(jsonPath("$.topFirstReactionTime.items[2].rank").value(3))
            }

            @Test
            @DisplayName("경쟁률 기준 정렬 (내림차순)")
            fun `get leaderboard sorted by competition rate descending`() {
                val now = Instant.now()
                leaderboardRecordRepository.saveAll(
                    listOf(
                        LeaderboardRecord(
                            userId = user1.id!!,
                            bestCompetitionRate = 2.5,
                            bestCompetitionRateAchievedAt = now,
                        ),
                        LeaderboardRecord(
                            userId = user2.id!!,
                            bestCompetitionRate = 5.0,
                            bestCompetitionRateAchievedAt = now,
                        ),
                        LeaderboardRecord(
                            userId = user3.id!!,
                            bestCompetitionRate = 3.8,
                            bestCompetitionRateAchievedAt = now,
                        ),
                    ),
                )

                mockMvc
                    .perform(get("/api/leaderboard"))
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.topCompetitionRate.items.length()").value(3))
                    .andExpect(jsonPath("$.topCompetitionRate.items[0].value").value(5.0))
                    .andExpect(jsonPath("$.topCompetitionRate.items[0].rank").value(1))
                    .andExpect(jsonPath("$.topCompetitionRate.items[1].value").value(3.8))
                    .andExpect(jsonPath("$.topCompetitionRate.items[1].rank").value(2))
                    .andExpect(jsonPath("$.topCompetitionRate.items[2].value").value(2.5))
                    .andExpect(jsonPath("$.topCompetitionRate.items[2].rank").value(3))
            }

            @Test
            @DisplayName("동일 반응시간 시 같은 랭크 부여")
            fun `tied reaction times get same rank`() {
                val now = Instant.now()
                leaderboardRecordRepository.saveAll(
                    listOf(
                        LeaderboardRecord(
                            userId = user1.id!!,
                            bestFirstReactionTime = 150,
                            bestFirstReactionTimeAchievedAt = now,
                        ),
                        LeaderboardRecord(
                            userId = user2.id!!,
                            bestFirstReactionTime = 150,
                            bestFirstReactionTimeAchievedAt = now.plusMillis(1000),
                        ),
                        LeaderboardRecord(
                            userId = user3.id!!,
                            bestFirstReactionTime = 200,
                            bestFirstReactionTimeAchievedAt = now,
                        ),
                    ),
                )

                mockMvc
                    .perform(get("/api/leaderboard"))
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.topFirstReactionTime.items[0].rank").value(1))
                    .andExpect(jsonPath("$.topFirstReactionTime.items[1].rank").value(1))
                    .andExpect(jsonPath("$.topFirstReactionTime.items[2].rank").value(3))
            }

            @Test
            @DisplayName("페이지네이션 동작 확인")
            fun `leaderboard pagination works`() {
                val now = Instant.now()
                leaderboardRecordRepository.saveAll(
                    listOf(
                        LeaderboardRecord(
                            userId = user1.id!!,
                            bestFirstReactionTime = 100,
                            bestFirstReactionTimeAchievedAt = now,
                        ),
                        LeaderboardRecord(
                            userId = user2.id!!,
                            bestFirstReactionTime = 200,
                            bestFirstReactionTimeAchievedAt = now,
                        ),
                        LeaderboardRecord(
                            userId = user3.id!!,
                            bestFirstReactionTime = 300,
                            bestFirstReactionTimeAchievedAt = now,
                        ),
                    ),
                )

                mockMvc
                    .perform(
                        get("/api/leaderboard")
                            .queryParam("page", "0")
                            .queryParam("size", "2"),
                    ).andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.topFirstReactionTime.items.length()").value(2))
                    .andExpect(jsonPath("$.topFirstReactionTime.pageInfo.page").value(0))
                    .andExpect(jsonPath("$.topFirstReactionTime.pageInfo.size").value(2))
                    .andExpect(jsonPath("$.topFirstReactionTime.pageInfo.totalElements").value(3))
                    .andExpect(jsonPath("$.topFirstReactionTime.pageInfo.totalPages").value(2))
                    .andExpect(jsonPath("$.topFirstReactionTime.pageInfo.hasNext").value(true))

                // 두 번째 페이지
                mockMvc
                    .perform(
                        get("/api/leaderboard")
                            .queryParam("page", "1")
                            .queryParam("size", "2"),
                    ).andExpect(status().isOk)
                    .andExpect(jsonPath("$.topFirstReactionTime.items.length()").value(1))
                    .andExpect(jsonPath("$.topFirstReactionTime.pageInfo.hasNext").value(false))
            }

            @Test
            @DisplayName("null 값인 항목은 해당 섹션에서 제외")
            fun `null values are excluded from section`() {
                val now = Instant.now()
                leaderboardRecordRepository.save(
                    LeaderboardRecord(
                        userId = user1.id!!,
                        bestFirstReactionTime = 100,
                        bestFirstReactionTimeAchievedAt = now,
                        bestSecondReactionTime = null,
                        bestCompetitionRate = null,
                    ),
                )

                mockMvc
                    .perform(get("/api/leaderboard"))
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.topFirstReactionTime.items.length()").value(1))
                    .andExpect(jsonPath("$.topSecondReactionTime.items.length()").value(0))
                    .andExpect(jsonPath("$.topCompetitionRate.items.length()").value(0))
            }
        }

        @Nested
        @DisplayName("내 리더보드 조회 (GET /api/leaderboard/me)")
        inner class GetMyLeaderboard {
            @Test
            @DisplayName("기록이 없는 사용자 조회 시 모든 값이 null")
            fun `get my leaderboard with no records returns all nulls`() {
                mockMvc
                    .perform(
                        get("/api/leaderboard/me")
                            .header("Authorization", "Bearer $token1"),
                    ).andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.bestFirstReactionTime").isEmpty)
                    .andExpect(jsonPath("$.bestFirstReactionTimeRank").isEmpty)
                    .andExpect(jsonPath("$.bestSecondReactionTime").isEmpty)
                    .andExpect(jsonPath("$.bestCompetitionRate").isEmpty)
            }

            @Test
            @DisplayName("기록이 있는 사용자 조회 시 값과 랭크 반환")
            fun `get my leaderboard with records returns values and ranks`() {
                val now = Instant.now()
                // user2가 더 빠른 기록 → user1 rank = 2
                leaderboardRecordRepository.saveAll(
                    listOf(
                        LeaderboardRecord(
                            userId = user1.id!!,
                            bestFirstReactionTime = 200,
                            bestFirstReactionTimeAchievedAt = now,
                            bestCompetitionRate = 3.0,
                            bestCompetitionRateAchievedAt = now,
                        ),
                        LeaderboardRecord(
                            userId = user2.id!!,
                            bestFirstReactionTime = 100,
                            bestFirstReactionTimeAchievedAt = now,
                            bestCompetitionRate = 5.0,
                            bestCompetitionRateAchievedAt = now,
                        ),
                    ),
                )

                mockMvc
                    .perform(
                        get("/api/leaderboard/me")
                            .header("Authorization", "Bearer $token1"),
                    ).andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.bestFirstReactionTime").value(200))
                    .andExpect(jsonPath("$.bestFirstReactionTimeRank").value(2))
                    .andExpect(jsonPath("$.bestCompetitionRate").value(3.0))
                    .andExpect(jsonPath("$.bestCompetitionRateRank").value(2))
            }

            @Test
            @DisplayName("비로그인 사용자 조회 시 401 반환")
            fun `get my leaderboard without auth returns 401`() {
                mockMvc
                    .perform(get("/api/leaderboard/me"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized)
            }
        }

        @Nested
        @DisplayName("주간 리더보드 조회 (GET /api/leaderboard/weekly)")
        inner class GetWeeklyLeaderboard {
            @Test
            @DisplayName("주간 리더보드 정상 조회")
            fun `get weekly leaderboard returns data`() {
                val now = Instant.now()
                weeklyLeaderboardRecordRepository.saveAll(
                    listOf(
                        WeeklyLeaderboardRecord(
                            userId = user1.id!!,
                            bestFirstReactionTime = 150,
                            bestFirstReactionTimeAchievedAt = now,
                        ),
                        WeeklyLeaderboardRecord(
                            userId = user2.id!!,
                            bestFirstReactionTime = 250,
                            bestFirstReactionTimeAchievedAt = now,
                        ),
                    ),
                )

                mockMvc
                    .perform(get("/api/leaderboard/weekly"))
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.topFirstReactionTime.items.length()").value(2))
                    .andExpect(jsonPath("$.topFirstReactionTime.items[0].value").value(150.0))
                    .andExpect(jsonPath("$.topFirstReactionTime.items[0].rank").value(1))
            }

            @Test
            @DisplayName("주간 내 기록 조회 - 비로그인 시 401")
            fun `get weekly my leaderboard without auth returns 401`() {
                mockMvc
                    .perform(get("/api/leaderboard/weekly/me"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized)
            }

            @Test
            @DisplayName("주간 내 기록 조회 - 기록 있는 경우")
            fun `get weekly my leaderboard with records`() {
                val now = Instant.now()
                weeklyLeaderboardRecordRepository.save(
                    WeeklyLeaderboardRecord(
                        userId = user1.id!!,
                        bestFirstReactionTime = 180,
                        bestFirstReactionTimeAchievedAt = now,
                    ),
                )

                mockMvc
                    .perform(
                        get("/api/leaderboard/weekly/me")
                            .header("Authorization", "Bearer $token1"),
                    ).andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.bestFirstReactionTime").value(180))
                    .andExpect(jsonPath("$.bestFirstReactionTimeRank").value(1))
            }
        }
    }
