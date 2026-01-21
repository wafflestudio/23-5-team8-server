package com.wafflestudio.team8server.practice

import com.wafflestudio.team8server.TestcontainersConfiguration
import com.wafflestudio.team8server.practice.service.PracticeSessionService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class PracticeSessionServiceTest
    @Autowired
    constructor(
        private val practiceSessionService: PracticeSessionService,
        private val redisTemplate: RedisTemplate<String, String>,
    ) {
        @BeforeEach
        fun setUp() {
            // Redis 초기화
            redisTemplate.keys("practice:session:*").forEach { key ->
                redisTemplate.delete(key)
            }
        }

        @Test
        @DisplayName("세션 생성 및 조회 성공")
        fun `create and get session successfully`() {
            val userId = 1L
            val practiceLogId = 100L

            practiceSessionService.createSession(userId, practiceLogId)

            val retrievedSessionId = practiceSessionService.getActiveSession(userId)

            assertEquals(practiceLogId, retrievedSessionId)
        }

        @Test
        @DisplayName("활성 세션 존재 여부 확인")
        fun `check active session exists`() {
            val userId = 1L
            val practiceLogId = 100L

            // 세션이 없을 때
            assertFalse(practiceSessionService.hasActiveSession(userId))

            // 세션 생성
            practiceSessionService.createSession(userId, practiceLogId)

            // 세션이 있을 때
            assertTrue(practiceSessionService.hasActiveSession(userId))
        }

        @Test
        @DisplayName("세션 종료")
        fun `end session successfully`() {
            val userId = 1L
            val practiceLogId = 100L

            // 세션 생성
            practiceSessionService.createSession(userId, practiceLogId)
            assertTrue(practiceSessionService.hasActiveSession(userId))

            // 세션 종료
            practiceSessionService.endSession(userId)

            // 세션이 삭제되었는지 확인
            assertFalse(practiceSessionService.hasActiveSession(userId))
            assertNull(practiceSessionService.getActiveSession(userId))
        }

        @Test
        @DisplayName("세션 없을 때 조회하면 null 반환")
        fun `get session returns null when no active session`() {
            val userId = 1L

            val retrievedSessionId = practiceSessionService.getActiveSession(userId)

            assertNull(retrievedSessionId)
        }

        @Test
        @DisplayName("세션 TTL 확인")
        fun `get session TTL successfully`() {
            val userId = 1L
            val practiceLogId = 100L

            practiceSessionService.createSession(userId, practiceLogId)

            val ttl = practiceSessionService.getSessionTTL(userId)

            assertNotNull(ttl)
            assertTrue(ttl!! > 0)
            assertTrue(ttl <= 300) // 5분 이하
        }

        @Test
        @DisplayName("세션 없을 때 TTL 조회하면 null 반환")
        fun `get session TTL returns null when no active session`() {
            val userId = 1L

            val ttl = practiceSessionService.getSessionTTL(userId)

            assertNull(ttl)
        }

        @Test
        @DisplayName("여러 사용자의 세션 독립적으로 관리")
        fun `manage multiple users sessions independently`() {
            val userId1 = 1L
            val practiceLogId1 = 100L

            val userId2 = 2L
            val practiceLogId2 = 200L

            // 두 사용자 세션 생성
            practiceSessionService.createSession(userId1, practiceLogId1)
            practiceSessionService.createSession(userId2, practiceLogId2)

            // 각각 올바른 세션 ID 반환
            assertEquals(practiceLogId1, practiceSessionService.getActiveSession(userId1))
            assertEquals(practiceLogId2, practiceSessionService.getActiveSession(userId2))

            // 한 사용자 세션 종료
            practiceSessionService.endSession(userId1)

            // 한 명만 세션이 없어짐
            assertFalse(practiceSessionService.hasActiveSession(userId1))
            assertTrue(practiceSessionService.hasActiveSession(userId2))
        }
    }
