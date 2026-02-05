package com.wafflestudio.team8server.user

import com.fasterxml.jackson.databind.ObjectMapper
import com.wafflestudio.team8server.TestcontainersConfiguration
import com.wafflestudio.team8server.common.time.MockTimeProvider
import com.wafflestudio.team8server.course.model.Course
import com.wafflestudio.team8server.course.model.Semester
import com.wafflestudio.team8server.course.repository.CourseRepository
import com.wafflestudio.team8server.practice.dto.PracticeAttemptRequest
import com.wafflestudio.team8server.practice.repository.PracticeDetailRepository
import com.wafflestudio.team8server.practice.repository.PracticeLogRepository
import com.wafflestudio.team8server.preenroll.dto.PreEnrollAddRequest
import com.wafflestudio.team8server.preenroll.dto.PreEnrollUpdateCartCountRequest
import com.wafflestudio.team8server.preenroll.repository.PreEnrollRepository
import com.wafflestudio.team8server.user.dto.ChangePasswordRequest
import com.wafflestudio.team8server.user.dto.LoginRequest
import com.wafflestudio.team8server.user.dto.SignupRequest
import com.wafflestudio.team8server.user.dto.UpdateProfileRequest
import com.wafflestudio.team8server.user.repository.LocalCredentialRepository
import com.wafflestudio.team8server.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@TestConfiguration
class MyPageTestConfig {
    @Bean
    @Primary
    fun mockTimeProvider(): MockTimeProvider = MockTimeProvider(currentTime = 1000000000L)
}

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class, MyPageTestConfig::class)
class MyPageControllerTest
    @Autowired
    constructor(
        private val webApplicationContext: WebApplicationContext,
        private val userRepository: UserRepository,
        private val localCredentialRepository: LocalCredentialRepository,
        private val practiceLogRepository: PracticeLogRepository,
        private val practiceDetailRepository: PracticeDetailRepository,
        private val courseRepository: CourseRepository,
        private val preEnrollRepository: PreEnrollRepository,
        private val mockTimeProvider: MockTimeProvider,
    ) {
        private lateinit var mockMvc: MockMvc
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
        }

        /**
         * 장바구니에 강의를 추가하고 cartCount를 설정합니다.
         */
        private fun addToCart(
            token: String,
            course: Course,
            cartCount: Int,
        ) {
            // 장바구니에 추가
            mockMvc.perform(
                post("/api/pre-enrolls")
                    .header("Authorization", "Bearer $token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(PreEnrollAddRequest(courseId = course.id!!))),
            )

            // cartCount 설정
            mockMvc.perform(
                patch("/api/pre-enrolls/${course.id}/cart-count")
                    .header("Authorization", "Bearer $token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(PreEnrollUpdateCartCountRequest(cartCount = cartCount))),
            )
        }

        private fun signupAndGetToken(
            email: String = "test@example.com",
            nickname: String = "테스터",
        ): String {
            val signupRequest =
                SignupRequest(
                    email = email,
                    password = "Test1234!",
                    nickname = nickname,
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

        // ==================== 마이페이지 조회 테스트 ====================

        @Nested
        @DisplayName("마이페이지 조회")
        inner class GetMyPage {
            @Test
            @DisplayName("마이페이지 조회 성공")
            fun `get mypage returns user profile`() {
                val token = signupAndGetToken(nickname = "홍길동")

                mockMvc
                    .perform(
                        get("/api/mypage")
                            .header("Authorization", "Bearer $token"),
                    ).andExpect(status().isOk)
                    .andExpect(jsonPath("$.nickname").value("홍길동"))
            }

            @Test
            @DisplayName("프로필 이미지 없이 마이페이지 조회")
            fun `get mypage without profile image`() {
                val token = signupAndGetToken(nickname = "테스터")

                mockMvc
                    .perform(
                        get("/api/mypage")
                            .header("Authorization", "Bearer $token"),
                    ).andExpect(status().isOk)
                    .andExpect(jsonPath("$.nickname").value("테스터"))
                    .andExpect(jsonPath("$.profileImageUrl").isEmpty)
            }

            @Test
            @DisplayName("인증 없이 마이페이지 조회 시 401 반환")
            fun `get mypage without auth returns 401`() {
                mockMvc
                    .perform(
                        get("/api/mypage"),
                    ).andExpect(status().isUnauthorized)
            }
        }

        // ==================== 프로필 수정 테스트 ====================

        @Nested
        @DisplayName("프로필 수정")
        inner class UpdateProfile {
            @Test
            @DisplayName("닉네임 수정 성공")
            fun `update nickname successfully`() {
                val token = signupAndGetToken(nickname = "기존닉네임")

                val request = UpdateProfileRequest(nickname = "새닉네임")

                mockMvc
                    .perform(
                        patch("/api/mypage/profile")
                            .header("Authorization", "Bearer $token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)),
                    ).andExpect(status().isOk)
                    .andExpect(jsonPath("$.nickname").value("새닉네임"))
            }

            @Test
            @DisplayName("인증 없이 프로필 수정 시 401 반환")
            fun `update profile without auth returns 401`() {
                val request = UpdateProfileRequest(nickname = "새닉네임")

                mockMvc
                    .perform(
                        patch("/api/mypage/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)),
                    ).andExpect(status().isUnauthorized)
            }
        }

        // ==================== 비밀번호 변경 테스트 ====================

        @Nested
        @DisplayName("비밀번호 변경")
        inner class ChangePassword {
            @Test
            @DisplayName("비밀번호 변경 성공")
            fun `change password successfully`() {
                val token = signupAndGetToken()

                val request =
                    ChangePasswordRequest(
                        currentPassword = "Test1234!",
                        newPassword = "NewPass1234!",
                    )

                mockMvc
                    .perform(
                        patch("/api/mypage/password")
                            .header("Authorization", "Bearer $token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)),
                    ).andExpect(status().isNoContent)

                // 새 비밀번호로 로그인 확인
                val loginRequest =
                    LoginRequest(
                        email = "test@example.com",
                        password = "NewPass1234!",
                    )

                mockMvc
                    .perform(
                        post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)),
                    ).andExpect(status().isOk)
                    .andExpect(jsonPath("$.accessToken").exists())
            }

            @Test
            @DisplayName("현재 비밀번호 불일치 시 401 반환")
            fun `change password with wrong current password returns 401`() {
                val token = signupAndGetToken()

                val request =
                    ChangePasswordRequest(
                        currentPassword = "WrongPass1!",
                        newPassword = "NewPass1234!",
                    )

                mockMvc
                    .perform(
                        patch("/api/mypage/password")
                            .header("Authorization", "Bearer $token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)),
                    ).andExpect(status().isUnauthorized)
                    .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
            }
        }

        // ==================== 연습 세션 목록 조회 테스트 ====================

        @Nested
        @DisplayName("연습 세션 목록 조회")
        inner class GetPracticeSessions {
            @Test
            @DisplayName("세션 없을 때 빈 목록 반환")
            fun `get practice sessions returns empty list when no sessions`() {
                val token = signupAndGetToken()

                mockMvc
                    .perform(
                        get("/api/mypage/practice-sessions")
                            .header("Authorization", "Bearer $token"),
                    ).andExpect(status().isOk)
                    .andExpect(jsonPath("$.items").isArray)
                    .andExpect(jsonPath("$.items").isEmpty)
                    .andExpect(jsonPath("$.pageInfo.totalElements").value(0))
            }

            @Test
            @DisplayName("세션 목록 조회 성공")
            fun `get practice sessions returns session list`() {
                val token = signupAndGetToken()

                // Course 생성
                val course =
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

                // 장바구니에 강의 추가
                addToCart(token, course, 100)

                // 연습 세션 생성
                mockMvc.perform(
                    post("/api/practice/start")
                        .header("Authorization", "Bearer $token"),
                )

                // 시도 추가
                mockTimeProvider.setTime(1000030050L)
                val request =
                    PracticeAttemptRequest(
                        courseId = course.id!!,
                    )
                mockMvc.perform(
                    post("/api/practice/attempt")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )

                mockMvc.perform(
                    post("/api/practice/end")
                        .header("Authorization", "Bearer $token"),
                )

                // 세션 목록 조회
                mockMvc
                    .perform(
                        get("/api/mypage/practice-sessions")
                            .header("Authorization", "Bearer $token"),
                    ).andExpect(status().isOk)
                    .andExpect(jsonPath("$.items").isArray)
                    .andExpect(jsonPath("$.items.length()").value(1))
                    .andExpect(jsonPath("$.items[0].id").isNumber)
                    .andExpect(jsonPath("$.items[0].practiceAt").exists())
                    .andExpect(jsonPath("$.items[0].totalAttempts").value(1))
                    .andExpect(jsonPath("$.items[0].successCount").value(1))
                    .andExpect(jsonPath("$.pageInfo.totalElements").value(1))
            }

            @Test
            @DisplayName("시도 횟수와 성공 횟수 포함된 세션 목록 조회")
            fun `get practice sessions with attempts and success count`() {
                val token = signupAndGetToken()

                // Course 생성
                val course =
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

                // 장바구니에 강의 추가
                addToCart(token, course, 100)

                // 세션 시작
                mockMvc.perform(
                    post("/api/practice/start")
                        .header("Authorization", "Bearer $token"),
                )

                // 수강신청 시도 (성공하도록 빠른 반응)
                mockTimeProvider.setTime(1000030050L) // userLatencyMs = 50ms
                val request =
                    PracticeAttemptRequest(
                        courseId = course.id!!,
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

                // 세션 목록 조회
                mockMvc
                    .perform(
                        get("/api/mypage/practice-sessions")
                            .header("Authorization", "Bearer $token"),
                    ).andExpect(status().isOk)
                    .andExpect(jsonPath("$.items[0].totalAttempts").value(1))
                    .andExpect(jsonPath("$.items[0].successCount").value(1))
            }

            @Test
            @DisplayName("페이지네이션 적용")
            fun `get practice sessions with pagination`() {
                val token = signupAndGetToken()

                // Course 생성
                val course =
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

                // 장바구니에 강의 추가
                addToCart(token, course, 100)

                // 3개의 연습 세션 생성 (각 세션마다 시도 하나씩)
                repeat(3) { i ->
                    mockMvc.perform(
                        post("/api/practice/start")
                            .header("Authorization", "Bearer $token"),
                    )

                    // 각 세션마다 다른 시간으로 설정 (attempt 성공을 위해 시간 전진 필요)
                    mockTimeProvider.setTime(1000030050L + i * 100000L)
                    val request =
                        PracticeAttemptRequest(
                            courseId = course.id!!,
                        )
                    mockMvc.perform(
                        post("/api/practice/attempt")
                            .header("Authorization", "Bearer $token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)),
                    )

                    mockMvc.perform(
                        post("/api/practice/end")
                            .header("Authorization", "Bearer $token"),
                    )
                }

                // 첫 번째 페이지 (size=2)
                mockMvc
                    .perform(
                        get("/api/mypage/practice-sessions")
                            .header("Authorization", "Bearer $token")
                            .param("page", "0")
                            .param("size", "2"),
                    ).andExpect(status().isOk)
                    .andExpect(jsonPath("$.items.length()").value(2))
                    .andExpect(jsonPath("$.pageInfo.page").value(0))
                    .andExpect(jsonPath("$.pageInfo.size").value(2))
                    .andExpect(jsonPath("$.pageInfo.totalElements").value(3))
                    .andExpect(jsonPath("$.pageInfo.totalPages").value(2))
                    .andExpect(jsonPath("$.pageInfo.hasNext").value(true))

                // 두 번째 페이지
                mockMvc
                    .perform(
                        get("/api/mypage/practice-sessions")
                            .header("Authorization", "Bearer $token")
                            .param("page", "1")
                            .param("size", "2"),
                    ).andExpect(status().isOk)
                    .andExpect(jsonPath("$.items.length()").value(1))
                    .andExpect(jsonPath("$.pageInfo.hasNext").value(false))
            }

            @Test
            @DisplayName("인증 없이 세션 목록 조회 시 401 반환")
            fun `get practice sessions without auth returns 401`() {
                mockMvc
                    .perform(
                        get("/api/mypage/practice-sessions"),
                    ).andExpect(status().isUnauthorized)
            }
        }

        // ==================== 연습 세션 상세 조회 테스트 ====================

        @Nested
        @DisplayName("연습 세션 상세 조회")
        inner class GetPracticeSessionDetail {
            @Test
            @DisplayName("세션 상세 조회 성공")
            fun `get practice session detail successfully`() {
                val token = signupAndGetToken()

                // Course 생성
                val course =
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

                // 장바구니에 강의 추가
                addToCart(token, course, 100)

                // 세션 시작
                val startResponse =
                    mockMvc
                        .perform(
                            post("/api/practice/start")
                                .header("Authorization", "Bearer $token"),
                        ).andReturn()

                val practiceLogId =
                    objectMapper
                        .readTree(startResponse.response.contentAsString)
                        .get("practiceLogId")
                        .asLong()

                // 수강신청 시도
                mockTimeProvider.setTime(1000030100L) // userLatencyMs = 100ms
                val request =
                    PracticeAttemptRequest(
                        courseId = course.id!!,
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

                // 세션 상세 조회
                mockMvc
                    .perform(
                        get("/api/mypage/practice-sessions/$practiceLogId")
                            .header("Authorization", "Bearer $token"),
                    ).andExpect(status().isOk)
                    .andExpect(jsonPath("$.practiceLogId").value(practiceLogId))
                    .andExpect(jsonPath("$.practiceAt").exists())
                    .andExpect(jsonPath("$.totalAttempts").value(1))
                    .andExpect(jsonPath("$.successCount").value(1))
                    .andExpect(jsonPath("$.attempts").isArray)
                    .andExpect(jsonPath("$.attempts.length()").value(1))
                    .andExpect(jsonPath("$.attempts[0].courseTitle").value("테스트 강의"))
                    .andExpect(jsonPath("$.attempts[0].isSuccess").value(true))
                    .andExpect(jsonPath("$.attempts[0].rank").isNumber)
                    .andExpect(jsonPath("$.attempts[0].percentile").isNumber)
                    .andExpect(jsonPath("$.attempts[0].reactionTime").isNumber)
            }

            @Test
            @DisplayName("존재하지 않는 세션 조회 시 404 반환")
            fun `get non-existent session returns 404`() {
                val token = signupAndGetToken()

                mockMvc
                    .perform(
                        get("/api/mypage/practice-sessions/99999")
                            .header("Authorization", "Bearer $token"),
                    ).andExpect(status().isNotFound)
                    .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
            }

            @Test
            @DisplayName("다른 사용자의 세션 조회 시 401 반환")
            fun `get other user's session returns 401`() {
                val token1 = signupAndGetToken(email = "user1@example.com")
                val token2 = signupAndGetToken(email = "user2@example.com")

                // user1의 세션 생성
                val startResponse =
                    mockMvc
                        .perform(
                            post("/api/practice/start")
                                .header("Authorization", "Bearer $token1"),
                        ).andReturn()

                val practiceLogId =
                    objectMapper
                        .readTree(startResponse.response.contentAsString)
                        .get("practiceLogId")
                        .asLong()

                mockMvc.perform(
                    post("/api/practice/end")
                        .header("Authorization", "Bearer $token1"),
                )

                // user2가 user1의 세션 조회 시도
                mockMvc
                    .perform(
                        get("/api/mypage/practice-sessions/$practiceLogId")
                            .header("Authorization", "Bearer $token2"),
                    ).andExpect(status().isUnauthorized)
                    .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
            }

            @Test
            @DisplayName("인증 없이 세션 상세 조회 시 401 반환")
            fun `get practice session detail without auth returns 401`() {
                mockMvc
                    .perform(
                        get("/api/mypage/practice-sessions/1"),
                    ).andExpect(status().isUnauthorized)
            }
        }

        // ==================== 회원 탈퇴 테스트 ====================

        @Nested
        @DisplayName("회원 탈퇴")
        inner class DeleteAccount {
            @Disabled("회원 탈퇴 로직 변경으로 인해 정비 전까지 임시 스킵")
            @Test
            @DisplayName("회원 탈퇴 성공")
            fun `delete account successfully`() {
                val token = signupAndGetToken()

                // 회원 탈퇴
                mockMvc
                    .perform(
                        delete("/api/mypage")
                            .header("Authorization", "Bearer $token"),
                    ).andExpect(status().isNoContent)

                // 탈퇴 후 마이페이지 조회 시 404 반환 확인
                mockMvc
                    .perform(
                        get("/api/mypage")
                            .header("Authorization", "Bearer $token"),
                    ).andExpect(status().isNotFound)
            }

            @Test
            @DisplayName("인증 없이 회원 탈퇴 시 401 반환")
            fun `delete account without auth returns 401`() {
                mockMvc
                    .perform(
                        delete("/api/mypage"),
                    ).andExpect(status().isUnauthorized)
            }
        }
    }
