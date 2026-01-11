package com.wafflestudio.team8server.practice

import com.fasterxml.jackson.databind.ObjectMapper
import com.wafflestudio.team8server.TestcontainersConfiguration
import com.wafflestudio.team8server.common.time.MockTimeProvider
import com.wafflestudio.team8server.course.model.Course
import com.wafflestudio.team8server.course.model.Semester
import com.wafflestudio.team8server.course.repository.CourseRepository
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
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@TestConfiguration
class PracticeTestConfig {
    @Bean
    @Primary
    fun mockTimeProvider(): MockTimeProvider = MockTimeProvider(currentTime = 1000000000L)
}

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class, PracticeTestConfig::class)
class PracticeControllerTest
    @Autowired
    constructor(
        private val webApplicationContext: WebApplicationContext,
        private val userRepository: UserRepository,
        private val localCredentialRepository: LocalCredentialRepository,
        private val practiceLogRepository: PracticeLogRepository,
        private val practiceDetailRepository: PracticeDetailRepository,
        private val courseRepository: CourseRepository,
        private val practiceSessionService: PracticeSessionService,
        private val mockTimeProvider: MockTimeProvider,
    ) {
        private lateinit var mockMvc: MockMvc
        private lateinit var savedCourse: Course
        private val objectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules()

        @BeforeEach
        fun setUp() {
            mockMvc =
                MockMvcBuilders
                    .webAppContextSetup(webApplicationContext)
                    .apply<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(springSecurity())
                    .build()

            // DB 및 세션 초기화
            practiceDetailRepository.deleteAll()
            practiceLogRepository.deleteAll()
            localCredentialRepository.deleteAll()
            userRepository.deleteAll()
            courseRepository.deleteAll()

            // Mock 시간 초기화 (기준 시간: 1000000000ms)
            mockTimeProvider.setTime(1000000000L)

            // 테스트용 Course 데이터 생성 (생성된 ID 저장)
            savedCourse =
                courseRepository.save(
                    Course(
                        year = 2025,
                        semester = Semester.SPRING,
                        courseNumber = "TEST001",
                        lectureNumber = "001",
                        courseTitle = "테스트 강의",
                        quota = 100,
                        instructor = "테스트 교수",
                    ),
                )
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
                    courseId = savedCourse.id!!,
                    userLatencyMs = 100,
                    totalCompetitors = 100,
                    capacity = 40,
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

            // 세션 시작 (현재 시간: 1000000000ms)
            mockMvc.perform(
                post("/api/practice/start")
                    .header("Authorization", "Bearer $token"),
            )

            // 5분 1초 경과 (300000ms + 1000ms = 301000ms)
            mockTimeProvider.advance(301000)

            val request =
                PracticeAttemptRequest(
                    courseId = savedCourse.id!!,
                    userLatencyMs = 100,
                    totalCompetitors = 100,
                    capacity = 40,
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
        @DisplayName("Early click (-1000ms ~ 0ms) - DB에 기록됨")
        fun `attempt practice with early click within threshold records to DB`() {
            val token = signupAndGetToken()

            // 세션 시작
            mockMvc.perform(
                post("/api/practice/start")
                    .header("Authorization", "Bearer $token"),
            )

            val request =
                PracticeAttemptRequest(
                    courseId = savedCourse.id!!,
                    userLatencyMs = -500, // -0.5초 (기록 범위 내)
                    totalCompetitors = 100,
                    capacity = 40,
                )

            mockMvc
                .perform(
                    post("/api/practice/attempt")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk) // 200
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("수강신청 시간이 아닙니다"))
        }

        @Test
        @DisplayName("Early click (< -1000ms) - DB에 기록 안 됨")
        fun `attempt practice with early click outside threshold does not record to DB`() {
            val token = signupAndGetToken()

            // 세션 시작
            mockMvc.perform(
                post("/api/practice/start")
                    .header("Authorization", "Bearer $token"),
            )

            val request =
                PracticeAttemptRequest(
                    courseId = savedCourse.id!!,
                    userLatencyMs = -2000, // -2초 (기록 범위 밖)
                    totalCompetitors = 100,
                    capacity = 40,
                )

            mockMvc
                .perform(
                    post("/api/practice/attempt")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk) // 200
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("수강신청 시간이 아닙니다"))
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
                    courseId = savedCourse.id!!,
                    userLatencyMs = 50, // 매우 빠른 반응 시간
                    totalCompetitors = 100,
                    capacity = 80, // 높은 정원
                )

            mockMvc
                .perform(
                    post("/api/practice/attempt")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk) // 200
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("수강신청에 성공했습니다"))
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
                    courseId = savedCourse.id!!,
                    userLatencyMs = 5000, // 느린 반응 시간
                    totalCompetitors = 100,
                    capacity = 10, // 낮은 정원
                )

            mockMvc
                .perform(
                    post("/api/practice/attempt")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk) // 200
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("정원이 초과되었습니다"))
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
                    courseId = savedCourse.id!!,
                    userLatencyMs = 100,
                    totalCompetitors = 100,
                    capacity = 40,
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
                    courseId = savedCourse.id!!,
                    userLatencyMs = 200,
                    totalCompetitors = 100,
                    capacity = 40,
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
                    courseId = savedCourse.id!!,
                    userLatencyMs = 100,
                    totalCompetitors = 0, // 유효하지 않은 값
                    capacity = 40,
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
