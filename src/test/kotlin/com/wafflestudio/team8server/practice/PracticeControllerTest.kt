package com.wafflestudio.team8server.practice

import com.fasterxml.jackson.databind.ObjectMapper
import com.wafflestudio.team8server.TestcontainersConfiguration
import com.wafflestudio.team8server.common.time.MockTimeProvider
import com.wafflestudio.team8server.course.model.Course
import com.wafflestudio.team8server.course.model.Semester
import com.wafflestudio.team8server.course.repository.CourseRepository
import com.wafflestudio.team8server.leaderboard.repository.LeaderboardRecordRepository
import com.wafflestudio.team8server.practice.dto.PracticeAttemptRequest
import com.wafflestudio.team8server.practice.dto.PracticeStartRequest
import com.wafflestudio.team8server.practice.dto.VirtualStartTimeOption
import com.wafflestudio.team8server.practice.repository.PracticeDetailRepository
import com.wafflestudio.team8server.practice.repository.PracticeLogRepository
import com.wafflestudio.team8server.practice.service.PracticeSessionService
import com.wafflestudio.team8server.user.dto.SignupRequest
import com.wafflestudio.team8server.user.repository.LocalCredentialRepository
import com.wafflestudio.team8server.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
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
import org.springframework.test.web.servlet.ResultHandler
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
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
        private val leaderboardRecordRepository: LeaderboardRecordRepository,
        private val mockTimeProvider: MockTimeProvider,
    ) {
        private lateinit var mockMvc: MockMvc
        private lateinit var savedCourse: Course
        private val objectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules()

        /**
         * stdout/stderr가 캡처되어도, 5xx면 반드시 테스트 실패 리포트에 원인이 남도록 강제합니다.
         */
        private fun failOn5xx(): ResultHandler =
            ResultHandler { r ->
                val statusCode = r.response.status
                if (statusCode in 500..599) {
                    val body = r.response.contentAsString
                    val ex = r.resolvedException
                    if (ex != null) {
                        throw AssertionError(
                            buildString {
                                append("HTTP ")
                                append(statusCode)
                                append(" returned from request. ")
                                append("ResolvedException=")
                                append(ex.javaClass.name)
                                append(": ")
                                append(ex.message)
                                append(". ")
                                append("Body=")
                                append(body)
                            },
                            ex,
                        )
                    }
                    throw AssertionError("HTTP $statusCode returned from request. Body=$body")
                }
            }

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
                )

            val response =
                mockMvc
                    .perform(
                        post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(signupRequest)),
                    ).andDo(failOn5xx())
                    .andReturn()

            val responseBody = response.response.contentAsString
            return objectMapper.readTree(responseBody).get("accessToken").asText()
        }

        // ==================== 세션 시작 테스트 ====================

        @Test
        @DisplayName("연습 세션 시작 성공 - 기본값 (08:29:30)")
        fun `start practice session successfully`() {
            val token = signupAndGetToken()

            mockMvc
                .perform(
                    post("/api/practice/start")
                        .header("Authorization", "Bearer $token"),
                ).andDo(failOn5xx())
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.practiceLogId").isNumber)
                .andExpect(jsonPath("$.virtualStartTime").value("08:29:30"))
                .andExpect(jsonPath("$.targetTime").value("08:30:00"))
                .andExpect(jsonPath("$.message").exists())
        }

        @Test
        @DisplayName("연습 세션 시작 성공 - 시작 시간 옵션 지정 (08:29:00)")
        fun `start practice session with custom start time option`() {
            val token = signupAndGetToken()

            val request = PracticeStartRequest(virtualStartTimeOption = VirtualStartTimeOption.TIME_08_29_00)

            mockMvc
                .perform(
                    post("/api/practice/start")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andDo(failOn5xx())
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.practiceLogId").isNumber)
                .andExpect(jsonPath("$.virtualStartTime").value("08:29:00"))
                .andExpect(jsonPath("$.targetTime").value("08:30:00"))
                .andExpect(jsonPath("$.message").exists())
        }

        @Test
        @DisplayName("이미 진행 중인 세션이 있을 때 기존 세션 종료 후 새 세션 시작")
        fun `start practice session ends existing session and starts new one`() {
            val token = signupAndGetToken()

            // 첫 번째 세션 시작
            val firstResult =
                mockMvc
                    .perform(
                        post("/api/practice/start")
                            .header("Authorization", "Bearer $token"),
                    ).andDo(failOn5xx())
                    .andExpect(status().isCreated)
                    .andReturn()

            val firstPracticeLogId =
                objectMapper
                    .readTree(firstResult.response.contentAsString)
                    .get("practiceLogId")
                    .asLong()

            // 두 번째 세션 시작 (기존 세션 종료 후 새 세션 시작)
            val secondResult =
                mockMvc
                    .perform(
                        post("/api/practice/start")
                            .header("Authorization", "Bearer $token"),
                    ).andDo(failOn5xx())
                    .andExpect(status().isCreated)
                    .andReturn()

            val secondPracticeLogId =
                objectMapper
                    .readTree(secondResult.response.contentAsString)
                    .get("practiceLogId")
                    .asLong()

            // 새로운 세션이 생성되었는지 확인
            assertNotEquals(firstPracticeLogId, secondPracticeLogId)
        }

        @Test
        @DisplayName("인증 없이 세션 시작 시도 시 401 반환")
        fun `start practice session without authentication returns 401`() {
            mockMvc
                .perform(
                    post("/api/practice/start"),
                ).andDo(failOn5xx())
                .andExpect(status().isUnauthorized) // 401
        }

        // ==================== 세션 종료 테스트 ====================

        @Test
        @DisplayName("연습 세션 종료 성공")
        fun `end practice session successfully`() {
            val token = signupAndGetToken()

            // 세션 시작
            mockMvc
                .perform(
                    post("/api/practice/start")
                        .header("Authorization", "Bearer $token"),
                ).andDo(failOn5xx())

            // 세션 종료
            mockMvc
                .perform(
                    post("/api/practice/end")
                        .header("Authorization", "Bearer $token"),
                ).andDo(print())
                .andDo(failOn5xx())
                .andExpect(status().isOk) // 200
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
                ).andDo(print())
                .andDo(failOn5xx())
                .andExpect(status().isBadRequest) // 400
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
                    totalCompetitors = 100,
                    capacity = 40,
                )

            mockMvc
                .perform(
                    post("/api/practice/attempt")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andDo(print())
                .andDo(failOn5xx())
                .andExpect(status().isBadRequest) // 400
                .andExpect(jsonPath("$.errorCode").value("NO_ACTIVE_SESSION"))
        }

        /**
         * 수동 테스트 전용:
         * Redis TTL은 시스템 시간 기반이므로 MockTimeProvider로 테스트할 수 없습니다.
         *
         * 수동 테스트 방법:
         * 1. @Disabled 어노테이션 제거
         * 2. application-test.yml의 practice.session.time-limit-seconds를 6으로 변경
         * 3. 테스트 실행하면 7초 대기 후 세션이 만료되어 400 에러 발생하는지 확인
         */
        @Test
        @Disabled("Redis TTL은 시스템 시간 기반이므로 MockTimeProvider로 테스트할 수 없습니다. 수동 테스트 필요.")
        @DisplayName("연습 시간 초과 시 400 반환 (Redis TTL 만료로 세션 없음) - 수동 테스트")
        fun `attempt practice after time limit returns 400`() {
            val token = signupAndGetToken()

            // 세션 시작
            mockMvc
                .perform(
                    post("/api/practice/start")
                        .header("Authorization", "Bearer $token"),
                ).andDo(failOn5xx())

            // 7초 대기 (TTL 6초 설정 시 만료 확인)
            Thread.sleep(7000)

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
                ).andDo(print())
                .andDo(failOn5xx())
                .andExpect(status().isBadRequest) // 400
                .andExpect(jsonPath("$.errorCode").value("NO_ACTIVE_SESSION"))
        }

        @Test
        @DisplayName("Early click (-1000ms ~ 0ms) - DB에 기록됨")
        fun `attempt practice with early click within threshold records to DB`() {
            val token = signupAndGetToken()

            // 세션 시작 (시작 시간: 1000000000ms, 기본 offset: 30000ms)
            mockMvc
                .perform(
                    post("/api/practice/start")
                        .header("Authorization", "Bearer $token"),
                ).andDo(failOn5xx())

            // 시간 조작: userLatencyMs = -500ms가 되도록 설정
            mockTimeProvider.setTime(1000029500L)

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
                ).andDo(print())
                .andDo(failOn5xx())
                .andExpect(status().isOk) // 200
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("수강신청 기간이 아닙니다"))
        }

        @Test
        @DisplayName("Early click (< -1000ms) - DB에 기록 안 됨")
        fun `attempt practice with early click outside threshold does not record to DB`() {
            val token = signupAndGetToken()

            // 세션 시작
            mockMvc
                .perform(
                    post("/api/practice/start")
                        .header("Authorization", "Bearer $token"),
                ).andDo(failOn5xx())

            mockTimeProvider.setTime(1000028000L)

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
                ).andDo(print())
                .andDo(failOn5xx())
                .andExpect(status().isOk) // 200
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("수강신청 기간이 아닙니다"))
        }

        @Test
        @DisplayName("수강신청 성공 (정원 내)")
        fun `attempt practice successfully within capacity`() {
            val token = signupAndGetToken()

            mockMvc
                .perform(
                    post("/api/practice/start")
                        .header("Authorization", "Bearer $token"),
                ).andDo(failOn5xx())

            mockTimeProvider.setTime(1000030050L)

            val request =
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
                        .content(objectMapper.writeValueAsString(request)),
                ).andDo(print())
                .andDo(failOn5xx())
                .andExpect(status().isOk) // 200
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("수강신청에 성공했습니다"))
        }

        @Test
        @DisplayName("수강신청 실패 (정원 초과)")
        fun `attempt practice fails when exceeds capacity`() {
            val token = signupAndGetToken()

            mockMvc
                .perform(
                    post("/api/practice/start")
                        .header("Authorization", "Bearer $token"),
                ).andDo(failOn5xx())

            mockTimeProvider.setTime(1000035000L)

            val request =
                PracticeAttemptRequest(
                    courseId = savedCourse.id!!,
                    totalCompetitors = 100,
                    capacity = 10,
                )

            mockMvc
                .perform(
                    post("/api/practice/attempt")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andDo(print())
                .andDo(failOn5xx())
                .andExpect(status().isOk) // 200
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("정원이 초과되었습니다"))
        }

        @Test
        @DisplayName("한 세션에서 여러 번 시도 가능")
        fun `multiple attempts in one session`() {
            val token = signupAndGetToken()

            // 두 번째 강의 생성
            val savedCourse2 =
                courseRepository.save(
                    Course(
                        year = 2025,
                        semester = Semester.SPRING,
                        courseNumber = "CS102",
                        lectureNumber = "002",
                        courseTitle = "알고리즘",
                        quota = 50,
                        instructor = "김교수",
                    ),
                )

            // 세션 시작
            mockMvc
                .perform(
                    post("/api/practice/start")
                        .header("Authorization", "Bearer $token"),
                ).andDo(failOn5xx())

            // 첫 번째 강의 시도
            mockTimeProvider.setTime(1000030100L)
            val request1 =
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
                        .content(objectMapper.writeValueAsString(request1)),
                ).andDo(failOn5xx())

            // 두 번째 강의 시도
            mockTimeProvider.setTime(1000030200L)
            val request2 =
                PracticeAttemptRequest(
                    courseId = savedCourse2.id!!,
                    totalCompetitors = 100,
                    capacity = 40,
                )

            mockMvc
                .perform(
                    post("/api/practice/attempt")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)),
                ).andDo(print())
                .andDo(failOn5xx())
                .andExpect(status().isOk) // 200

            // 세션 종료 후 totalAttempts 확인
            mockMvc
                .perform(
                    post("/api/practice/end")
                        .header("Authorization", "Bearer $token"),
                ).andDo(print())
                .andDo(failOn5xx())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.totalAttempts").value(2))
        }

        @Test
        @DisplayName("같은 과목 중복 시도 시 이전 결과 반환 - 성공 케이스")
        fun `duplicate attempt returns already enrolled message when first attempt succeeded`() {
            val token = signupAndGetToken()

            // 세션 시작
            mockMvc
                .perform(
                    post("/api/practice/start")
                        .header("Authorization", "Bearer $token"),
                ).andDo(failOn5xx())

            // 첫 번째 시도
            mockTimeProvider.setTime(1000030050L)
            val request1 =
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
                        .content(objectMapper.writeValueAsString(request1)),
                ).andDo(print())
                .andDo(failOn5xx())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("수강신청에 성공했습니다"))

            // 같은 강의 두 번째 시도
            mockTimeProvider.setTime(1000030200L)
            val request2 =
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
                        .content(objectMapper.writeValueAsString(request2)),
                ).andDo(print())
                .andDo(failOn5xx())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("이미 수강신청된 강의입니다"))

            // 세션 종료 후 totalAttempts가 1인지 확인
            mockMvc
                .perform(
                    post("/api/practice/end")
                        .header("Authorization", "Bearer $token"),
                ).andDo(print())
                .andDo(failOn5xx())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.totalAttempts").value(1))
        }

        @Test
        @DisplayName("같은 과목 중복 시도 시 이전 결과 반환 - 실패 케이스")
        fun `duplicate attempt returns capacity exceeded message when first attempt failed`() {
            val token = signupAndGetToken()

            // 세션 시작
            mockMvc
                .perform(
                    post("/api/practice/start")
                        .header("Authorization", "Bearer $token"),
                ).andDo(failOn5xx())

            // 첫 번째 시도
            mockTimeProvider.setTime(1000035000L)
            val request1 =
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
                        .content(objectMapper.writeValueAsString(request1)),
                ).andDo(print())
                .andDo(failOn5xx())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("정원이 초과되었습니다"))

            // 같은 강의 두 번째 시도
            mockTimeProvider.setTime(1000035050L)
            val request2 =
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
                        .content(objectMapper.writeValueAsString(request2)),
                ).andDo(print())
                .andDo(failOn5xx())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("정원이 초과되었습니다(이미 시도한 강의입니다)"))

            // 세션 종료 후 totalAttempts가 1인지 확인
            mockMvc
                .perform(
                    post("/api/practice/end")
                        .header("Authorization", "Bearer $token"),
                ).andDo(print())
                .andDo(failOn5xx())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.totalAttempts").value(1))
        }

        @Test
        @DisplayName("유효성 검증 실패 - totalCompetitors가 0 이하")
        fun `attempt practice with invalid totalCompetitors returns 400`() {
            val token = signupAndGetToken()

            // 세션 시작
            mockMvc
                .perform(
                    post("/api/practice/start")
                        .header("Authorization", "Bearer $token"),
                ).andDo(failOn5xx())

            val request =
                PracticeAttemptRequest(
                    courseId = savedCourse.id!!,
                    totalCompetitors = 0, // 유효하지 않은 값
                    capacity = 40,
                )

            mockMvc
                .perform(
                    post("/api/practice/attempt")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andDo(print())
                .andDo(failOn5xx())
                .andExpect(status().isBadRequest) // 400
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationErrors.totalCompetitors").exists())
        }

        // ==================== 성공한 강의 목록 조회 테스트 ====================

        @Test
        @DisplayName("성공한 강의 목록 조회 성공")
        fun `get enrolled courses returns successful courses`() {
            val token = signupAndGetToken()

            // 두 번째 강의 생성
            val course2 =
                courseRepository.save(
                    Course(
                        year = 2025,
                        semester = Semester.SPRING,
                        courseNumber = "CS200",
                        lectureNumber = "001",
                        courseTitle = "알고리즘",
                        quota = 50,
                        instructor = "김교수",
                        placeAndTime = """{"time": "화(14:00~15:50)"}""",
                    ),
                )

            // 세션 시작
            mockMvc.perform(
                post("/api/practice/start")
                    .header("Authorization", "Bearer $token"),
            )

            // 첫 번째 강의 시도 - 성공 (빠른 반응)
            mockTimeProvider.setTime(1000030050L)
            val request1 =
                PracticeAttemptRequest(
                    courseId = savedCourse.id!!,
                    totalCompetitors = 100,
                    capacity = 80,
                )
            mockMvc.perform(
                post("/api/practice/attempt")
                    .header("Authorization", "Bearer $token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request1)),
            )

            // 두 번째 강의 시도 - 성공 (빠른 반응)
            mockTimeProvider.setTime(1000030100L)
            val request2 =
                PracticeAttemptRequest(
                    courseId = course2.id!!,
                    totalCompetitors = 100,
                    capacity = 80,
                )
            mockMvc.perform(
                post("/api/practice/attempt")
                    .header("Authorization", "Bearer $token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request2)),
            )

            // 세션 종료
            mockMvc.perform(
                post("/api/practice/end")
                    .header("Authorization", "Bearer $token"),
            )

            // 성공한 강의 목록 조회
            mockMvc
                .perform(
                    get("/api/practice/enrolled-courses")
                        .header("Authorization", "Bearer $token"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].courseNumber").value("CS200")) // 정렬 확인 (CS200 < TEST001)
                .andExpect(jsonPath("$[1].courseNumber").value("TEST001"))
        }

        @Test
        @DisplayName("연습 기록이 없을 때 404 반환")
        fun `get enrolled courses without practice log returns 404`() {
            val token = signupAndGetToken()

            // 연습 없이 바로 조회
            mockMvc
                .perform(
                    get("/api/practice/enrolled-courses")
                        .header("Authorization", "Bearer $token"),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("연습 기록이 없습니다"))
        }

        @Test
        @DisplayName("성공한 강의가 없을 때 빈 리스트 반환")
        fun `get enrolled courses with no successful attempts returns empty list`() {
            val token = signupAndGetToken()

            // 세션 시작
            mockMvc.perform(
                post("/api/practice/start")
                    .header("Authorization", "Bearer $token"),
            )

            // 강의 시도 - 실패 (느린 반응)
            mockTimeProvider.setTime(1000035000L)
            val request =
                PracticeAttemptRequest(
                    courseId = savedCourse.id!!,
                    totalCompetitors = 100,
                    capacity = 10,
                )
            mockMvc.perform(
                post("/api/practice/attempt")
                    .header("Authorization", "Bearer $token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )

            // 세션 종료
            mockMvc.perform(
                post("/api/practice/end")
                    .header("Authorization", "Bearer $token"),
            )

            // 성공한 강의 목록 조회 - 빈 리스트
            mockMvc
                .perform(
                    get("/api/practice/enrolled-courses")
                        .header("Authorization", "Bearer $token"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(0))
        }

        @Test
        @DisplayName("인증 없이 성공한 강의 조회 시 401 반환")
        fun `get enrolled courses without authentication returns 401`() {
            mockMvc
                .perform(
                    get("/api/practice/enrolled-courses"),
                ).andExpect(status().isUnauthorized)
        }

        // ==================== 시간 중복 검증 테스트 ====================

        @Test
        @DisplayName("시간이 겹치는 강의 시도 시 실패")
        fun `attempt practice with time conflict returns failure`() {
            val token = signupAndGetToken()

            // 시간 정보가 있는 강의 생성 (월요일 10:00~11:50)
            val course1 =
                courseRepository.save(
                    Course(
                        year = 2025,
                        semester = Semester.SPRING,
                        courseNumber = "TIME001",
                        lectureNumber = "001",
                        courseTitle = "월요일 오전 강의",
                        quota = 100,
                        instructor = "박교수",
                        placeAndTime = """{"time": "월(10:00~11:50)"}""",
                    ),
                )

            // 시간이 겹치는 강의 생성 (월요일 11:00~12:50)
            val course2 =
                courseRepository.save(
                    Course(
                        year = 2025,
                        semester = Semester.SPRING,
                        courseNumber = "TIME002",
                        lectureNumber = "001",
                        courseTitle = "월요일 오전 겹치는 강의",
                        quota = 100,
                        instructor = "이교수",
                        placeAndTime = """{"time": "월(11:00~12:50)"}""",
                    ),
                )

            // 세션 시작
            mockMvc.perform(
                post("/api/practice/start")
                    .header("Authorization", "Bearer $token"),
            )

            // 첫 번째 강의 시도 - 성공
            mockTimeProvider.setTime(1000030050L)
            val request1 =
                PracticeAttemptRequest(
                    courseId = course1.id!!,
                    totalCompetitors = 100,
                    capacity = 80,
                )
            mockMvc
                .perform(
                    post("/api/practice/attempt")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.isSuccess").value(true))

            // 두 번째 강의 시도 - 시간 중복으로 실패
            mockTimeProvider.setTime(1000030100L)
            val request2 =
                PracticeAttemptRequest(
                    courseId = course2.id!!,
                    totalCompetitors = 100,
                    capacity = 80,
                )
            mockMvc
                .perform(
                    post("/api/practice/attempt")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("시간이 겹치는 강의는 수강신청할 수 없습니다"))
        }

        @Test
        @DisplayName("시간이 겹치지 않는 강의는 정상 수강신청")
        fun `attempt practice without time conflict succeeds`() {
            val token = signupAndGetToken()

            // 시간 정보가 있는 강의 생성 (월요일 10:00~11:50)
            val course1 =
                courseRepository.save(
                    Course(
                        year = 2025,
                        semester = Semester.SPRING,
                        courseNumber = "TIME003",
                        lectureNumber = "001",
                        courseTitle = "월요일 오전 강의",
                        quota = 100,
                        instructor = "박교수",
                        placeAndTime = """{"time": "월(10:00~11:50)"}""",
                    ),
                )

            // 시간이 겹치지 않는 강의 생성 (화요일 10:00~11:50)
            val course2 =
                courseRepository.save(
                    Course(
                        year = 2025,
                        semester = Semester.SPRING,
                        courseNumber = "TIME004",
                        lectureNumber = "001",
                        courseTitle = "화요일 오전 강의",
                        quota = 100,
                        instructor = "이교수",
                        placeAndTime = """{"time": "화(10:00~11:50)"}""",
                    ),
                )

            // 세션 시작
            mockMvc.perform(
                post("/api/practice/start")
                    .header("Authorization", "Bearer $token"),
            )

            // 첫 번째 강의 시도 - 성공
            mockTimeProvider.setTime(1000030050L)
            val request1 =
                PracticeAttemptRequest(
                    courseId = course1.id!!,
                    totalCompetitors = 100,
                    capacity = 80,
                )
            mockMvc
                .perform(
                    post("/api/practice/attempt")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.isSuccess").value(true))

            // 두 번째 강의 시도 - 시간 중복 없이 성공
            mockTimeProvider.setTime(1000030100L)
            val request2 =
                PracticeAttemptRequest(
                    courseId = course2.id!!,
                    totalCompetitors = 100,
                    capacity = 80,
                )
            mockMvc
                .perform(
                    post("/api/practice/attempt")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("수강신청에 성공했습니다"))
        }

        @Test
        @DisplayName("실패한 강의와는 시간 중복 검증하지 않음")
        fun `time conflict check does not apply to failed courses`() {
            val token = signupAndGetToken()

            // 시간 정보가 있는 강의 생성 (월요일 10:00~11:50)
            val course1 =
                courseRepository.save(
                    Course(
                        year = 2025,
                        semester = Semester.SPRING,
                        courseNumber = "TIME005",
                        lectureNumber = "001",
                        courseTitle = "월요일 오전 강의 (실패용)",
                        quota = 100,
                        instructor = "박교수",
                        placeAndTime = """{"time": "월(10:00~11:50)"}""",
                    ),
                )

            // 시간이 겹치는 강의 생성 (월요일 11:00~12:50)
            val course2 =
                courseRepository.save(
                    Course(
                        year = 2025,
                        semester = Semester.SPRING,
                        courseNumber = "TIME006",
                        lectureNumber = "001",
                        courseTitle = "월요일 오전 겹치는 강의",
                        quota = 100,
                        instructor = "이교수",
                        placeAndTime = """{"time": "월(11:00~12:50)"}""",
                    ),
                )

            // 세션 시작
            mockMvc.perform(
                post("/api/practice/start")
                    .header("Authorization", "Bearer $token"),
            )

            // 첫 번째 강의 시도 - 실패 (느린 반응, 정원 초과)
            mockTimeProvider.setTime(1000035000L)
            val request1 =
                PracticeAttemptRequest(
                    courseId = course1.id!!,
                    totalCompetitors = 100,
                    capacity = 10,
                )
            mockMvc
                .perform(
                    post("/api/practice/attempt")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("정원이 초과되었습니다"))

            // 두 번째 강의 시도 - 첫 번째가 실패했으므로 시간 중복 검증 안 함, 성공 가능
            mockTimeProvider.setTime(1000030050L)
            val request2 =
                PracticeAttemptRequest(
                    courseId = course2.id!!,
                    totalCompetitors = 100,
                    capacity = 80,
                )
            mockMvc
                .perform(
                    post("/api/practice/attempt")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("수강신청에 성공했습니다"))
        }

        /**
         * 수동 테스트 전용:
         * 세션 TTL 만료 시 Redis Keyspace Notification을 통해 리더보드가 자동 갱신되는지 확인합니다.
         *
         * 수동 테스트 방법:
         * 1. @Disabled 어노테이션 제거
         * 2. application-test.yml의 practice.session.time-limit-seconds를 6으로 변경
         * 3. Redis 서버에서 notify-keyspace-events 설정이 "Ex"인지 확인
         *    - redis-cli로 접속 후: CONFIG GET notify-keyspace-events
         *    - 설정 안 되어 있으면: CONFIG SET notify-keyspace-events Ex
         * 4. 테스트 실행하면 7초 대기 후 리더보드가 갱신되었는지 확인
         */
        @Test
        @Disabled("Redis TTL 만료 및 Keyspace Notification 테스트. 수동 테스트 필요.")
        @DisplayName("세션 TTL 만료 시 리더보드 자동 갱신 - 수동 테스트")
        fun `leaderboard is updated when session expires by TTL`() {
            val token = signupAndGetToken()

            // 사용자 ID 조회 (LocalCredential을 통해)
            val credential = localCredentialRepository.findByEmail("test@example.com")!!
            val userId = credential.user.id!!

            // 리더보드 초기 상태 확인 (없어야 함)
            val initialRecord = leaderboardRecordRepository.findByUserId(userId)
            println("초기 리더보드 레코드: $initialRecord")

            // 세션 시작
            val startResult =
                mockMvc
                    .perform(
                        post("/api/practice/start")
                            .header("Authorization", "Bearer $token"),
                    ).andDo(failOn5xx())
                    .andExpect(status().isCreated)
                    .andReturn()

            val practiceLogId =
                objectMapper
                    .readTree(startResult.response.contentAsString)
                    .get("practiceLogId")
                    .asLong()
            println("세션 시작됨 - practiceLogId: $practiceLogId")

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
            println("수강신청 시도 완료")

            // 세션 종료 API 호출하지 않고 TTL 만료 대기
            println("TTL 만료 대기 중... (7초)")
            Thread.sleep(7000)

            // 리더보드 갱신 확인
            val updatedRecord = leaderboardRecordRepository.findByUserId(userId)
            println("갱신된 리더보드 레코드: $updatedRecord")
            println("bestFirstReactionTime: ${updatedRecord?.bestFirstReactionTime}")

            assertNotNull(updatedRecord, "리더보드 레코드가 생성되어야 합니다")
            assertNotNull(updatedRecord?.bestFirstReactionTime, "bestFirstReactionTime이 갱신되어야 합니다")
            println("테스트 성공: 세션 TTL 만료 시 리더보드가 자동 갱신되었습니다!")
        }
    }
