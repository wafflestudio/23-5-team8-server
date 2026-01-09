package com.wafflestudio.team8server.practice

import com.fasterxml.jackson.databind.ObjectMapper
import com.wafflestudio.team8server.TestcontainersConfiguration
import com.wafflestudio.team8server.practice.dto.PracticeAttemptRequest
import com.wafflestudio.team8server.practice.repository.PracticeDetailRepository
import com.wafflestudio.team8server.practice.repository.PracticeLogRepository
import com.wafflestudio.team8server.practice.service.PracticeSessionService
import com.wafflestudio.team8server.user.dto.SignupRequest
import com.wafflestudio.team8server.user.repository.LocalCredentialRepository
import com.wafflestudio.team8server.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class PracticeControllerTest
    @Autowired
    constructor(
        private val webApplicationContext: WebApplicationContext,
        private val userRepository: UserRepository,
        private val localCredentialRepository: LocalCredentialRepository,
        private val practiceLogRepository: PracticeLogRepository,
        private val practiceDetailRepository: PracticeDetailRepository,
        private val practiceSessionService: PracticeSessionService,
    ) {
        private lateinit var mockMvc: MockMvc
        private val objectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules()

        @BeforeEach
        fun setUp() {
            mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()

            // DB 및 세션 초기화
            practiceDetailRepository.deleteAll()
            practiceLogRepository.deleteAll()
            localCredentialRepository.deleteAll()
            userRepository.deleteAll()
        }

        /**
         * 테스트용 사용자를 회원가입하고 JWT 토큰을 반환합니다.
         */
        private fun signupAndGetToken(email: String = "test@example.com"): String {
            val signupRequest =
                SignupRequest(
                    email = email,
                    password = "Test1234!",
                    nickname = "테스터",
                    profileImageUrl = null,
                )

            val response =
                mockMvc
                    .perform(
                        post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(signupRequest)),
                    ).andReturn()

            val responseBody = response.response.contentAsString
            return objectMapper.readTree(responseBody).get("accessToken").asText()
        }

        // ==================== 세션 시작 테스트 ====================

        @Test
        @DisplayName("연습 세션 시작 성공")
        fun `start practice session successfully`() {
            val token = signupAndGetToken()

            mockMvc
                .perform(
                    post("/api/practice/start")
                        .header("Authorization", "Bearer $token"),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.practiceLogId").isNumber)
                .andExpect(jsonPath("$.virtualStartTime").value("08:28:00"))
                .andExpect(jsonPath("$.targetTime").value("08:30:00"))
                .andExpect(jsonPath("$.timeLimit").value("08:33:00"))
                .andExpect(jsonPath("$.message").exists())
        }

        @Test
        @DisplayName("이미 진행 중인 세션이 있을 때 세션 시작 실패")
        fun `start practice session fails when active session exists`() {
            val token = signupAndGetToken()

            // 첫 번째 세션 시작
            mockMvc.perform(
                post("/api/practice/start")
                    .header("Authorization", "Bearer $token"),
            )

            // 두 번째 세션 시작 시도 (실패해야 함)
            mockMvc
                .perform(
                    post("/api/practice/start")
                        .header("Authorization", "Bearer $token"),
                ).andExpect(status().isConflict) // 409
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.errorCode").value("ACTIVE_SESSION_EXISTS"))
        }

        @Test
        @DisplayName("인증 없이 세션 시작 시도 시 401 반환")
        fun `start practice session without authentication returns 401`() {
            mockMvc
                .perform(
                    post("/api/practice/start"),
                ).andExpect(status().isUnauthorized) // 401
        }

        // ==================== 세션 종료 테스트 ====================

        @Test
        @DisplayName("연습 세션 종료 성공")
        fun `end practice session successfully`() {
            val token = signupAndGetToken()

            // 세션 시작
            mockMvc.perform(
                post("/api/practice/start")
                    .header("Authorization", "Bearer $token"),
            )

            // 세션 종료
            mockMvc
                .perform(
                    post("/api/practice/end")
                        .header("Authorization", "Bearer $token"),
                ).andExpect(status().isOk) // 200
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.totalAttempts").isNumber)
        }

        @Test
        @DisplayName("활성 세션 없이 세션 종료 시도 시 400 반환")
        fun `end practice session without active session returns 400`() {
            val token = signupAndGetToken()

            mockMvc
                .perform(
                    post("/api/practice/end")
                        .header("Authorization", "Bearer $token"),
                ).andExpect(status().isBadRequest) // 400
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("NO_ACTIVE_SESSION"))
        }

        // ==================== 수강신청 시도 테스트 ====================

        @Test
        @DisplayName("활성 세션 없이 수강신청 시도 시 400 반환")
        fun `attempt practice without active session returns 400`() {
            val token = signupAndGetToken()

            val request =
                PracticeAttemptRequest(
                    courseId = 1L,
                    userLatencyMs = 100,
                    currentVirtualTime = "08:30:01",
                    totalCompetitors = 100,
                    capacity = 40,
                    scale = 5.0,
                    shape = 0.5,
                )

            mockMvc
                .perform(
                    post("/api/practice/attempt")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isBadRequest) // 400
                .andExpect(jsonPath("$.errorCode").value("NO_ACTIVE_SESSION"))
        }

        @Test
        @DisplayName("연습 시간 초과 시 400 반환")
        fun `attempt practice after time limit returns 400`() {
            val token = signupAndGetToken()

            // 세션 시작
            mockMvc.perform(
                post("/api/practice/start")
                    .header("Authorization", "Bearer $token"),
            )

            val request =
                PracticeAttemptRequest(
                    courseId = 1L,
                    userLatencyMs = 100,
                    currentVirtualTime = "08:33:01", // 08:33:00 이후
                    totalCompetitors = 100,
                    capacity = 40,
                    scale = 5.0,
                    shape = 0.5,
                )

            mockMvc
                .perform(
                    post("/api/practice/attempt")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isBadRequest) // 400
                .andExpect(jsonPath("$.errorCode").value("PRACTICE_TIME_EXPIRED"))
        }

        @Test
        @DisplayName("Early click (-5000ms ~ 0ms) - DB에 기록됨")
        fun `attempt practice with early click within threshold records to DB`() {
            val token = signupAndGetToken()

            // 세션 시작
            mockMvc.perform(
                post("/api/practice/start")
                    .header("Authorization", "Bearer $token"),
            )

            val request =
                PracticeAttemptRequest(
                    courseId = 1L,
                    userLatencyMs = -1500, // -1.5초 (기록 범위 내)
                    currentVirtualTime = "08:30:00",
                    totalCompetitors = 100,
                    capacity = 40,
                    scale = 5.0,
                    shape = 0.5,
                )

            mockMvc
                .perform(
                    post("/api/practice/attempt")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk) // 200
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.rank").isEmpty)
                .andExpect(jsonPath("$.percentile").isEmpty)
                .andExpect(jsonPath("$.reactionTime").value(0))
                .andExpect(jsonPath("$.earlyClickDiff").value(-1500))
        }

        @Test
        @DisplayName("Early click (< -5000ms) - DB에 기록 안 됨")
        fun `attempt practice with early click outside threshold does not record to DB`() {
            val token = signupAndGetToken()

            // 세션 시작
            mockMvc.perform(
                post("/api/practice/start")
                    .header("Authorization", "Bearer $token"),
            )

            val request =
                PracticeAttemptRequest(
                    courseId = 1L,
                    userLatencyMs = -6000, // -6초 (기록 범위 밖)
                    currentVirtualTime = "08:29:54",
                    totalCompetitors = 100,
                    capacity = 40,
                    scale = 5.0,
                    shape = 0.5,
                )

            mockMvc
                .perform(
                    post("/api/practice/attempt")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk) // 200
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.earlyClickDiff").isEmpty)
        }

        @Test
        @DisplayName("수강신청 성공 (정원 내)")
        fun `attempt practice successfully within capacity`() {
            val token = signupAndGetToken()

            // 세션 시작
            mockMvc.perform(
                post("/api/practice/start")
                    .header("Authorization", "Bearer $token"),
            )

            val request =
                PracticeAttemptRequest(
                    courseId = 1L,
                    userLatencyMs = 50, // 매우 빠른 반응 시간
                    currentVirtualTime = "08:30:01",
                    totalCompetitors = 100,
                    capacity = 80, // 높은 정원
                    scale = 5.0,
                    shape = 0.5,
                )

            mockMvc
                .perform(
                    post("/api/practice/attempt")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk) // 200
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.rank").isNumber)
                .andExpect(jsonPath("$.percentile").isNumber)
                .andExpect(jsonPath("$.reactionTime").value(50))
                .andExpect(jsonPath("$.message").exists())
        }

        @Test
        @DisplayName("수강신청 실패 (정원 초과)")
        fun `attempt practice fails when exceeds capacity`() {
            val token = signupAndGetToken()

            // 세션 시작
            mockMvc.perform(
                post("/api/practice/start")
                    .header("Authorization", "Bearer $token"),
            )

            val request =
                PracticeAttemptRequest(
                    courseId = 1L,
                    userLatencyMs = 5000, // 느린 반응 시간
                    currentVirtualTime = "08:30:05",
                    totalCompetitors = 100,
                    capacity = 10, // 낮은 정원
                    scale = 5.0,
                    shape = 0.5,
                )

            mockMvc
                .perform(
                    post("/api/practice/attempt")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk) // 200
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.rank").isNumber)
                .andExpect(jsonPath("$.percentile").isNumber)
                .andExpect(jsonPath("$.reactionTime").value(5000))
                .andExpect(jsonPath("$.message").exists())
        }

        @Test
        @DisplayName("한 세션에서 여러 번 시도 가능")
        fun `multiple attempts in one session`() {
            val token = signupAndGetToken()

            // 세션 시작
            mockMvc.perform(
                post("/api/practice/start")
                    .header("Authorization", "Bearer $token"),
            )

            // 첫 번째 시도
            val request1 =
                PracticeAttemptRequest(
                    courseId = 1L,
                    userLatencyMs = 100,
                    currentVirtualTime = "08:30:01",
                    totalCompetitors = 100,
                    capacity = 40,
                    scale = 5.0,
                    shape = 0.5,
                )

            mockMvc.perform(
                post("/api/practice/attempt")
                    .header("Authorization", "Bearer $token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request1)),
            )

            // 두 번째 시도
            val request2 =
                PracticeAttemptRequest(
                    courseId = 2L,
                    userLatencyMs = 200,
                    currentVirtualTime = "08:30:02",
                    totalCompetitors = 100,
                    capacity = 40,
                    scale = 5.0,
                    shape = 0.5,
                )

            mockMvc
                .perform(
                    post("/api/practice/attempt")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)),
                ).andExpect(status().isOk) // 200

            // 세션 종료 후 totalAttempts 확인
            mockMvc
                .perform(
                    post("/api/practice/end")
                        .header("Authorization", "Bearer $token"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.totalAttempts").value(2))
        }

        @Test
        @DisplayName("유효성 검증 실패 - totalCompetitors가 0 이하")
        fun `attempt practice with invalid totalCompetitors returns 400`() {
            val token = signupAndGetToken()

            // 세션 시작
            mockMvc.perform(
                post("/api/practice/start")
                    .header("Authorization", "Bearer $token"),
            )

            val request =
                PracticeAttemptRequest(
                    courseId = 1L,
                    userLatencyMs = 100,
                    currentVirtualTime = "08:30:01",
                    totalCompetitors = 0, // 유효하지 않은 값
                    capacity = 40,
                    scale = 5.0,
                    shape = 0.5,
                )

            mockMvc
                .perform(
                    post("/api/practice/attempt")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isBadRequest) // 400
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationErrors.totalCompetitors").exists())
        }
    }
