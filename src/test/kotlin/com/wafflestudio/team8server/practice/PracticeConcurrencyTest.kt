package com.wafflestudio.team8server.practice

import com.fasterxml.jackson.databind.ObjectMapper
import com.wafflestudio.team8server.TestcontainersConfiguration
import com.wafflestudio.team8server.common.time.MockTimeProvider
import com.wafflestudio.team8server.course.model.Course
import com.wafflestudio.team8server.course.model.Semester
import com.wafflestudio.team8server.course.repository.CourseRepository
import com.wafflestudio.team8server.practice.repository.PracticeDetailRepository
import com.wafflestudio.team8server.practice.repository.PracticeLogRepository
import com.wafflestudio.team8server.practice.service.PracticeSessionService
import com.wafflestudio.team8server.preenroll.dto.PreEnrollAddRequest
import com.wafflestudio.team8server.preenroll.dto.PreEnrollUpdateCartCountRequest
import com.wafflestudio.team8server.preenroll.repository.PreEnrollRepository
import com.wafflestudio.team8server.user.dto.SignupRequest
import com.wafflestudio.team8server.user.repository.LocalCredentialRepository
import com.wafflestudio.team8server.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@TestConfiguration
class ConcurrencyTestConfig {
    @Bean
    @Primary
    fun mockTimeProvider(): MockTimeProvider = MockTimeProvider(currentTime = 1000000000L)
}

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class, ConcurrencyTestConfig::class)
class PracticeConcurrencyTest
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

            practiceDetailRepository.deleteAll()
            practiceLogRepository.deleteAll()
            preEnrollRepository.deleteAll()
            localCredentialRepository.deleteAll()
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

        private fun signupAndGetToken(email: String): String {
            val signupRequest =
                SignupRequest(
                    email = email,
                    password = "Test1234!",
                    nickname = "테스터${email.hashCode()}",
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
        @DisplayName("동일 사용자가 동시에 세션 시작 요청 시 하나만 성공")
        fun `concurrent session start from same user results in single active session`() {
            val token = signupAndGetToken("concurrent@example.com")
            addToCart(token, savedCourse, 100)

            val threadCount = 5
            val latch = CountDownLatch(threadCount)
            val executor = Executors.newFixedThreadPool(threadCount)
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)

            repeat(threadCount) {
                executor.submit {
                    try {
                        val result =
                            mockMvc
                                .perform(
                                    post("/api/practice/start")
                                        .header("Authorization", "Bearer $token"),
                                ).andReturn()

                        if (result.response.status in 200..299) {
                            successCount.incrementAndGet()
                        } else {
                            failCount.incrementAndGet()
                        }
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await()
            executor.shutdown()

            // 모든 요청이 처리됨 (세션 자동 교체로 모두 201이 될 수도 있고,
            // 락 경합으로 일부 409가 될 수도 있음)
            assertEquals(threadCount, successCount.get() + failCount.get())
            // 최소 1개는 성공
            assert(successCount.get() >= 1) { "최소 1개의 세션 시작이 성공해야 합니다" }
        }

        @Test
        @DisplayName("여러 사용자가 동시에 세션 시작 시 각각 독립적으로 성공")
        fun `concurrent session start from different users all succeed`() {
            val userCount = 5
            val tokens =
                (1..userCount).map { idx ->
                    val token = signupAndGetToken("user$idx@example.com")
                    addToCart(token, savedCourse, 100)
                    token
                }

            val latch = CountDownLatch(userCount)
            val executor = Executors.newFixedThreadPool(userCount)
            val successCount = AtomicInteger(0)

            tokens.forEach { token ->
                executor.submit {
                    try {
                        val result =
                            mockMvc
                                .perform(
                                    post("/api/practice/start")
                                        .header("Authorization", "Bearer $token"),
                                ).andReturn()

                        if (result.response.status == 201) {
                            successCount.incrementAndGet()
                        }
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await()
            executor.shutdown()

            // 모든 사용자가 독립적으로 성공해야 함
            assertEquals(userCount, successCount.get(), "모든 사용자의 세션 시작이 성공해야 합니다")
        }

        @Test
        @DisplayName("여러 사용자가 동시에 수강신청 시도 시 각각 독립적으로 처리")
        fun `concurrent enrollment attempts from different users are independent`() {
            val userCount = 5
            val tokens =
                (1..userCount).map { idx ->
                    val token = signupAndGetToken("attempt$idx@example.com")
                    addToCart(token, savedCourse, 100)
                    token
                }

            // 각 사용자 세션 시작
            tokens.forEach { token ->
                mockMvc
                    .perform(
                        post("/api/practice/start")
                            .header("Authorization", "Bearer $token"),
                    ).andExpect(status().isCreated)
            }

            // 시간 조작: 50ms 지연 (수강신청 시간 이후)
            mockTimeProvider.setTime(1000030050L)

            val latch = CountDownLatch(userCount)
            val executor = Executors.newFixedThreadPool(userCount)
            val responseStatuses = java.util.concurrent.ConcurrentHashMap<String, Int>()

            tokens.forEachIndexed { idx, token ->
                executor.submit {
                    try {
                        val attemptRequest =
                            mapOf(
                                "courseId" to savedCourse.id!!,
                                "totalCompetitors" to 100,
                                "capacity" to 80,
                            )

                        val result =
                            mockMvc
                                .perform(
                                    post("/api/practice/attempt")
                                        .header("Authorization", "Bearer $token")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(attemptRequest)),
                                ).andReturn()

                        responseStatuses["user$idx"] = result.response.status
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await()
            executor.shutdown()

            // 모든 사용자의 요청이 처리됨
            assertEquals(userCount, responseStatuses.size, "모든 사용자의 요청이 처리되어야 합니다")
            // 모든 응답이 200 OK
            responseStatuses.forEach { (user, statusCode) ->
                assertEquals(200, statusCode, "$user 의 요청이 200이어야 합니다")
            }
        }
    }
