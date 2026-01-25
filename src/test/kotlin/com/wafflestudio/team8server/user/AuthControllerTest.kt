package com.wafflestudio.team8server.user

import com.fasterxml.jackson.databind.ObjectMapper
import com.wafflestudio.team8server.TestcontainersConfiguration
import com.wafflestudio.team8server.user.dto.LoginRequest
import com.wafflestudio.team8server.user.dto.SignupRequest
import com.wafflestudio.team8server.user.dto.SocialLoginRequest
import com.wafflestudio.team8server.user.repository.LocalCredentialRepository
import com.wafflestudio.team8server.user.repository.UserRepository
import com.wafflestudio.team8server.user.service.TokenBlacklistService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class AuthControllerTest
    @Autowired
    constructor(
        private val webApplicationContext: WebApplicationContext, // Spring 컨텍스트
        private val userRepository: UserRepository, // 테스트 후 DB 정리용
        private val localCredentialRepository: LocalCredentialRepository,
        private val tokenBlacklistService: TokenBlacklistService,
    ) {
        private lateinit var mockMvc: MockMvc // HTTP 요청 시뮬레이션

        // JSON 직렬화/역직렬화 (Kotlin 및 Java 8 Time 지원)
        private val objectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules()

        @BeforeEach
        fun setUp() {
            // WebApplicationContext로부터 MockMvc 빌드 (Spring Security 적용)
            mockMvc =
                MockMvcBuilders
                    .webAppContextSetup(webApplicationContext)
                    .apply<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(springSecurity())
                    .build()
            localCredentialRepository.deleteAll()
            userRepository.deleteAll()
        }

        @Test
        @DisplayName("유효한 정보로 회원가입 성공")
        fun `signup with valid data returns 201`() {
            val request =
                SignupRequest(
                    email = "test@example.com",
                    password = "Test1234!", // 영문+숫자+특수문자
                    nickname = "테스터",
                )

            mockMvc
                .perform(
                    post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.user.id").isNumber) // id는 숫자
                .andExpect(jsonPath("$.user.nickname").value("테스터"))
        }

        @Test
        @DisplayName("프로필 이미지 URL 포함 회원가입 성공")
        fun `signup with profile image URL returns 201`() {
            val request =
                SignupRequest(
                    email = "user@test.com",
                    password = "SecurePass123!",
                    nickname = "유저",
                )

            mockMvc
                .perform(
                    post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isCreated)
        }

        @Test
        @DisplayName("이메일 중복 시 409 CONFLICT 반환")
        fun `signup with duplicate email returns 409`() {
            val existingRequest =
                SignupRequest(
                    email = "duplicate@test.com",
                    password = "Password1!",
                    nickname = "기존유저",
                )
            mockMvc.perform(
                post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(existingRequest)),
            )

            val duplicateRequest =
                SignupRequest(
                    email = "duplicate@test.com", // 동일한 이메일
                    password = "DifferentPass1!",
                    nickname = "새유저",
                )

            mockMvc
                .perform(
                    post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)),
                ).andExpect(status().isConflict) // 409
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_EMAIL"))
                .andExpect(jsonPath("$.message").exists())
        }

        @Test
        @DisplayName("잘못된 이메일 형식 시 400 BAD_REQUEST 반환")
        fun `signup with invalid email format returns 400`() {
            val request =
                SignupRequest(
                    email = "invalid-email", // @ 없음
                    password = "Password1!",
                    nickname = "유저",
                )

            mockMvc
                .perform(
                    post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isBadRequest) // 400
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationErrors.email").exists()) // email 필드 에러
        }

        @Test
        @DisplayName("약한 비밀번호 시 400 BAD_REQUEST 반환")
        fun `signup with weak password returns 400`() {
            val request =
                SignupRequest(
                    email = "user@test.com",
                    password = "weak1234", // 특수문자 없음
                    nickname = "유저",
                )

            mockMvc
                .perform(
                    post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.password").exists()) // password 필드 에러
                .andExpect(jsonPath("$.validationErrors.password").value("비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다"))
        }

        @Test
        @DisplayName("닉네임이 너무 짧거나 길 때 400 BAD_REQUEST 반환")
        fun `signup with invalid nickname length returns 400`() {
            val request =
                SignupRequest(
                    email = "user@test.com",
                    password = "Password1!",
                    nickname = "A", // 1자
                )

            mockMvc
                .perform(
                    post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.validationErrors.nickname").exists())
        }

        @Test
        @DisplayName("유효한 정보로 로그인 성공")
        fun `login with valid credentials returns 200`() {
            val signupRequest =
                SignupRequest(
                    email = "login@test.com",
                    password = "Password1!",
                    nickname = "로그인테스터",
                )
            mockMvc.perform(
                post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signupRequest)),
            )

            val loginRequest =
                LoginRequest(
                    email = "login@test.com",
                    password = "Password1!",
                )

            mockMvc
                .perform(
                    post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)),
                ).andExpect(status().isOk) // 200
                .andExpect(jsonPath("$.accessToken").exists()) // JWT 토큰 존재
                .andExpect(jsonPath("$.accessToken").isString) // 문자열
                .andExpect(jsonPath("$.user.id").isNumber) // 사용자 ID
                .andExpect(jsonPath("$.user.nickname").value("로그인테스터"))
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 로그인 시 401 UNAUTHORIZED 반환")
        fun `login with non-existent email returns 401`() {
            val loginRequest =
                LoginRequest(
                    email = "nonexistent@test.com",
                    password = "Password1!",
                )

            val json = objectMapper.writeValueAsString(loginRequest)
            println("DEBUG: JSON = $json") // 디버깅용

            mockMvc
                .perform(
                    post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json),
                ).andDo(print()) // 응답 출력 (디버깅용)
                .andExpect(status().isUnauthorized) // 401
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
        }

        @Test
        @DisplayName("잘못된 비밀번호로 로그인 시 401 UNAUTHORIZED 반환")
        fun `login with wrong password returns 401`() {
            val signupRequest =
                SignupRequest(
                    email = "wrongpass@test.com",
                    password = "CorrectPassword1!",
                    nickname = "사용자",
                )
            mockMvc.perform(
                post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signupRequest)),
            )

            val loginRequest =
                LoginRequest(
                    email = "wrongpass@test.com",
                    password = "WrongPassword1!",
                )

            mockMvc
                .perform(
                    post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)),
                ).andExpect(status().isUnauthorized) // 401
        }

        @Test
        @DisplayName("카카오 소셜 로그인 시 요청 바디가 비어있다면 400 BAD_REQUEST 반환")
        fun `kakao social login with empty body returns 400`() {
            mockMvc
                .perform(
                    post("/api/auth/kakao/login")
                        .contentType(MediaType.APPLICATION_JSON),
                ).andDo(print())
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
        }

        @Test
        @DisplayName("카카오 소셜 로그인 시 code가 누락되면 400 BAD_REQUEST 반환")
        fun `kakao social login with blank code returns 400`() {
            val request = SocialLoginRequest(code = "", redirectUri = null)

            mockMvc
                .perform(
                    post("/api/auth/kakao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andDo(print())
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationErrors.code").exists())
        }

        @Test
        @DisplayName("구글 소셜 로그인 시 요청 바디가 비어있다면 400 BAD_REQUEST 반환")
        fun `google social login with empty body returns 400`() {
            mockMvc
                .perform(
                    post("/api/auth/google/login")
                        .contentType(MediaType.APPLICATION_JSON),
                ).andDo(print())
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
        }

        @Test
        @DisplayName("구글 소셜 로그인 시 code가 누락되면 400 BAD_REQUEST 반환")
        fun `google social login with blank code returns 400`() {
            val request = SocialLoginRequest(code = "", redirectUri = null)

            mockMvc
                .perform(
                    post("/api/auth/google/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andDo(print())
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationErrors.code").exists())
        }

        @Test
        @DisplayName("로그아웃 후 해당 토큰으로 인증 API 호출 시 401 반환")
        fun `blacklisted token cannot access protected resources`() {
            // 회원가입하고 토큰 받기
            val signupRequest =
                SignupRequest(
                    email = "blacklist@test.com",
                    password = "Password1!",
                    nickname = "테스터",
                )
            val signupResponse =
                mockMvc
                    .perform(
                        post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(signupRequest)),
                    ).andDo(print())
                    .andExpect(status().isCreated)
                    .andReturn()

            val responseBody = signupResponse.response.contentAsString
            val accessToken = objectMapper.readTree(responseBody).get("accessToken").asText()

            // 로그아웃 전 마이페이지 접근 가능 확인
            mockMvc
                .perform(
                    get("/api/mypage")
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)

            // 로그아웃
            mockMvc
                .perform(
                    post("/api/auth/logout")
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isNoContent)

            // 로그아웃 후 같은 토큰으로 마이페이지 접근 시 401
            mockMvc
                .perform(
                    get("/api/mypage")
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isUnauthorized)
        }
    }
