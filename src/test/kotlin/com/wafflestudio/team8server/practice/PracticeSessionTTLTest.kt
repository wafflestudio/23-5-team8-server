package com.wafflestudio.team8server.practice

import com.fasterxml.jackson.databind.ObjectMapper
import com.wafflestudio.team8server.TestcontainersConfiguration
import com.wafflestudio.team8server.common.time.MockTimeProvider
import com.wafflestudio.team8server.course.model.Course
import com.wafflestudio.team8server.course.model.Semester
import com.wafflestudio.team8server.course.repository.CourseRepository
import com.wafflestudio.team8server.leaderboard.repository.LeaderboardRecordRepository
import com.wafflestudio.team8server.practice.dto.PracticeAttemptRequest
import com.wafflestudio.team8server.practice.repository.PracticeDetailRepository
import com.wafflestudio.team8server.practice.repository.PracticeLogRepository
import com.wafflestudio.team8server.practice.service.PracticeSessionService
import com.wafflestudio.team8server.preenroll.dto.PreEnrollAddRequest
import com.wafflestudio.team8server.preenroll.dto.PreEnrollUpdateCartCountRequest
import com.wafflestudio.team8server.preenroll.repository.PreEnrollRepository
import com.wafflestudio.team8server.user.dto.SignupRequest
import com.wafflestudio.team8server.user.repository.LocalCredentialRepository
import com.wafflestudio.team8server.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertNotNull
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
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultHandler
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@TestConfiguration
class TTLTestConfig {
    @Bean
    @Primary
    fun mockTimeProvider(): MockTimeProvider = MockTimeProvider(currentTime = 1000000000L)
}

/**
 * Redis TTL 만료 테스트
 *
 * 기존 PracticeControllerTest에서 @Disabled로 분리되어 있던 테스트를
 * 짧은 TTL(3초) 설정으로 자동화합니다.
 *
 * Thread.sleep을 사용하므로 실행 시간이 다소 길 수 있습니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class, TTLTestConfig::class)
@TestPropertySource(properties = ["practice.session.time-limit-seconds=3"])
class PracticeSessionTTLTest
    @Autowired
    constructor(
        private val webApplicationContext: WebApplicationContext,
        private val userRepository: UserRepository,
        private val localCredentialRepository: LocalCredentialRepository,
        private val practiceLogRepository: PracticeLogRepository,
        private val practiceDetailRepository: PracticeDetailRepository,
        private val courseRepository: CourseRepository,
        private val preEnrollRepository: PreEnrollRepository,
        private val practiceSessionService: PracticeSessionService,
        private val leaderboardRecordRepository: LeaderboardRecordRepository,
        private val mockTimeProvider: MockTimeProvider,
    ) {
        private lateinit var mockMvc: MockMvc
        private lateinit var savedCourse: Course
        private val objectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules()

        private fun failOn5xx(): ResultHandler =
            ResultHandler { r ->
                val statusCode = r.response.status
                if (statusCode in 500..599) {
                    val body = r.response.contentAsString
                    val ex = r.resolvedException
                    if (ex != null) {
                        throw AssertionError("HTTP $statusCode: ${ex.message}. Body=$body", ex)
                    }
                    throw AssertionError("HTTP $statusCode. Body=$body")
                }
            }

        @BeforeEach
        fun setUp() {
            mockMvc =
                MockMvcBuilders
                    .webAppContextSetup(webApplicationContext)
                    .apply<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(springSecurity())
                    .build()

            practiceDetailRepository.deleteAll()
            practiceLogRepository.deleteAll()
            preEnrollRepository.deleteAll()
            localCredentialRepository.deleteAll()
            leaderboardRecordRepository.deleteAll()
            userRepository.deleteAll()
            courseRepository.deleteAll()

            mockTimeProvider.setTime(1000000000L)

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

        private fun signupAndGetToken(email: String = "test@example.com"): String {
            val signupRequest =
                SignupRequest(
                    email = email,
                    password = "Test1234!",
                    nickname = "테스터",
                )

            val response =
                mockMvc
                    .perform(
                        post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(signupRequest)),
                    ).andDo(failOn5xx())
                    .andReturn()

            return objectMapper.readTree(response.response.contentAsString).get("accessToken").asText()
        }

        private fun addToCart(
            token: String,
            course: Course,
            cartCount: Int,
        ) {
            mockMvc.perform(
                post("/api/pre-enrolls")
                    .header("Authorization", "Bearer $token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(PreEnrollAddRequest(courseId = course.id!!))),
            )

            mockMvc.perform(
                patch("/api/pre-enrolls/${course.id}/cart-count")
                    .header("Authorization", "Bearer $token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(PreEnrollUpdateCartCountRequest(cartCount = cartCount))),
            )
        }

        @Test
        @DisplayName("Redis TTL 만료 후 수강신청 시도 시 400 NO_ACTIVE_SESSION 반환")
        fun `attempt practice after TTL expiration returns 400`() {
            val token = signupAndGetToken()
            addToCart(token, savedCourse, 100)

            // 세션 시작 (TTL = 3초)
            mockMvc
                .perform(
                    post("/api/practice/start")
                        .header("Authorization", "Bearer $token"),
                ).andDo(failOn5xx())
                .andExpect(status().isCreated)

            // TTL 만료 대기 (3초 + 여유 1초)
            Thread.sleep(4000)

            // 시간 조작: 수강신청 시간 이후
            mockTimeProvider.setTime(1000030050L)

            val request =
                PracticeAttemptRequest(
                    courseId = savedCourse.id!!,
                    totalCompetitors = 100,
                    capacity = 40,
                )

            mockMvc
                .perform(
                    post("/api/practice/attempt")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andDo(failOn5xx())
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.errorCode").value("NO_ACTIVE_SESSION"))
        }

        @Test
        @DisplayName("세션 TTL 만료 시 Keyspace Notification으로 리더보드 자동 갱신")
        fun `leaderboard is updated when session expires by TTL`() {
            val token = signupAndGetToken()
            addToCart(token, savedCourse, 100)

            val credential = localCredentialRepository.findByEmail("test@example.com")!!
            val userId = credential.user.id!!

            // 세션 시작 (TTL = 3초)
            mockMvc
                .perform(
                    post("/api/practice/start")
                        .header("Authorization", "Bearer $token"),
                ).andDo(failOn5xx())
                .andExpect(status().isCreated)

            // 수강신청 시도 (리더보드 갱신 대상 데이터 생성)
            mockTimeProvider.setTime(1000030050L) // 50ms 지연
            val attemptRequest =
                PracticeAttemptRequest(
                    courseId = savedCourse.id!!,
                    totalCompetitors = 100,
                    capacity = 80,
                )
            mockMvc
                .perform(
                    post("/api/practice/attempt")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attemptRequest)),
                ).andDo(failOn5xx())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.isSuccess").value(true))

            // 세션 종료 API 호출하지 않고 TTL 만료 대기 (3초 + 여유 2초)
            Thread.sleep(5000)

            // 리더보드 갱신 확인
            val updatedRecord = leaderboardRecordRepository.findByUserId(userId)
            assertNotNull(updatedRecord, "리더보드 레코드가 생성되어야 합니다")
            assertNotNull(updatedRecord?.bestFirstReactionTime, "bestFirstReactionTime이 갱신되어야 합니다")
        }
    }
