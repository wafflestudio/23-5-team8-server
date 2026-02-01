package com.wafflestudio.team8server.practice

import com.wafflestudio.team8server.TestcontainersConfiguration
import com.wafflestudio.team8server.practice.model.PracticeDetail
import com.wafflestudio.team8server.practice.model.PracticeLog
import com.wafflestudio.team8server.practice.repository.PracticeDetailRepository
import com.wafflestudio.team8server.practice.repository.PracticeLogRepository
import com.wafflestudio.team8server.practice.service.ReactionTimePercentileService
import com.wafflestudio.team8server.user.model.User
import com.wafflestudio.team8server.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class ReactionTimePercentileServiceTest
    @Autowired
    constructor(
        private val reactionTimePercentileService: ReactionTimePercentileService,
        private val practiceDetailRepository: PracticeDetailRepository,
        private val practiceLogRepository: PracticeLogRepository,
        private val userRepository: UserRepository,
    ) {
        private lateinit var savedUser: User
        private lateinit var savedPracticeLog: PracticeLog

        @BeforeEach
        fun setUp() {
            practiceDetailRepository.deleteAll()
            practiceLogRepository.deleteAll()
            userRepository.deleteAll()

            savedUser = userRepository.save(User(nickname = "TestUser"))
            savedPracticeLog = practiceLogRepository.save(PracticeLog(user = savedUser))
        }

        private fun createPracticeDetail(reactionTime: Int): PracticeDetail =
            PracticeDetail(
                practiceLog = savedPracticeLog,
                course = null,
                courseTitle = "Test Course",
                lectureNumber = "001",
                isSuccess = true,
                reactionTime = reactionTime,
                rank = 1,
                percentile = 0.0,
                capacity = 10,
                totalCompetitors = 100,
            )

        @Nested
        @DisplayName("calculatePercentile")
        inner class CalculatePercentileTest {
            @Test
            @DisplayName("데이터가 없으면 0.5를 반환한다")
            fun `returns 0_5 when no data exists`() {
                reactionTimePercentileService.refreshData()

                val percentile = reactionTimePercentileService.calculatePercentile(100)

                assertEquals(0.5, percentile)
            }

            @Test
            @DisplayName("데이터가 있으면 이진 탐색으로 백분위를 계산한다")
            fun `calculates percentile using binary search when data exists`() {
                // Given: 10개의 반응 시간 데이터 (100, 200, 300, ..., 1000)
                for (i in 1..10) {
                    practiceDetailRepository.save(createPracticeDetail(reactionTime = i * 100))
                }
                reactionTimePercentileService.refreshData()

                // When & Then
                // 50ms -> 모든 데이터보다 빠름 -> 0.0 (상위 0%)
                assertEquals(0.0, reactionTimePercentileService.calculatePercentile(50))

                // 100ms -> 1개와 같음, insertionPoint=0 -> 0.0
                assertEquals(0.0, reactionTimePercentileService.calculatePercentile(100))

                // 150ms -> 1개보다 느림, insertionPoint=1 -> 0.1 (상위 10%)
                assertEquals(0.1, reactionTimePercentileService.calculatePercentile(150))

                // 500ms -> 5개와 같음, insertionPoint=4 -> 0.4 (상위 40%)
                assertEquals(0.4, reactionTimePercentileService.calculatePercentile(500))

                // 1500ms -> 모든 데이터보다 느림 -> 1.0 (상위 100%)
                assertEquals(1.0, reactionTimePercentileService.calculatePercentile(1500))
            }
        }

        @Nested
        @DisplayName("calculateRank")
        inner class CalculateRankTest {
            @Test
            @DisplayName("백분위와 경쟁자 수로 등수를 계산한다")
            fun `calculates rank from percentile and total competitors`() {
                // 상위 5% -> 100명 중 5등
                assertEquals(5, reactionTimePercentileService.calculateRank(0.05, 100))

                // 상위 50% -> 100명 중 50등
                assertEquals(50, reactionTimePercentileService.calculateRank(0.50, 100))

                // 상위 0% -> 최소 1등
                assertEquals(1, reactionTimePercentileService.calculateRank(0.0, 100))
            }
        }

        @Nested
        @DisplayName("isSuccessful")
        inner class IsSuccessfulTest {
            @Test
            @DisplayName("등수가 정원 이하면 성공")
            fun `returns true when rank is within capacity`() {
                assertTrue(reactionTimePercentileService.isSuccessful(rank = 1, capacity = 10))
                assertTrue(reactionTimePercentileService.isSuccessful(rank = 10, capacity = 10))
            }

            @Test
            @DisplayName("등수가 정원 초과면 실패")
            fun `returns false when rank exceeds capacity`() {
                assertFalse(reactionTimePercentileService.isSuccessful(rank = 11, capacity = 10))
                assertFalse(reactionTimePercentileService.isSuccessful(rank = 100, capacity = 10))
            }
        }
    }
