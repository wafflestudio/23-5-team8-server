package com.wafflestudio.team8server.notice

import com.fasterxml.jackson.databind.ObjectMapper
import com.wafflestudio.team8server.TestcontainersConfiguration
import com.wafflestudio.team8server.notice.dto.CreateNoticeRequest
import com.wafflestudio.team8server.notice.model.Notice
import com.wafflestudio.team8server.notice.repository.NoticeRepository
import com.wafflestudio.team8server.user.JwtTokenProvider
import com.wafflestudio.team8server.user.model.User
import com.wafflestudio.team8server.user.model.UserRole
import com.wafflestudio.team8server.user.repository.UserRepository
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class NoticeControllerTest
    @Autowired
    constructor(
        private val webApplicationContext: WebApplicationContext,
        private val noticeRepository: NoticeRepository,
        private val userRepository: UserRepository,
        private val jwtTokenProvider: JwtTokenProvider,
    ) {
        private lateinit var mockMvc: MockMvc
        private val objectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules()

        private lateinit var adminUser: User
        private lateinit var normalUser: User
        private lateinit var adminToken: String
        private lateinit var userToken: String

        @BeforeEach
        fun setUp() {
            mockMvc =
                MockMvcBuilders
                    .webAppContextSetup(webApplicationContext)
                    .apply<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(springSecurity())
                    .build()

            noticeRepository.deleteAll()
            userRepository.deleteAll()

            // 관리자 사용자 생성
            adminUser =
                userRepository.save(
                    User(
                        nickname = "관리자",
                        role = UserRole.ADMIN,
                    ),
                )
            adminToken = jwtTokenProvider.createToken(adminUser.id, "ADMIN")

            // 일반 사용자 생성
            normalUser =
                userRepository.save(
                    User(
                        nickname = "일반사용자",
                        role = UserRole.USER,
                    ),
                )
            userToken = jwtTokenProvider.createToken(normalUser.id, "USER")
        }

        @Test
        @DisplayName("비로그인 사용자도 공지사항 목록 조회 가능")
        fun `get notices without auth returns 200`() {
            noticeRepository.save(
                Notice(
                    title = "테스트 공지",
                    content = "테스트 내용",
                    isPinned = false,
                ),
            )

            mockMvc
                .perform(get("/api/notices"))
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.items").isArray)
                .andExpect(jsonPath("$.items.length()").value(1))
        }

        @Test
        @DisplayName("비로그인 사용자도 공지사항 상세 조회 가능")
        fun `get notice detail without auth returns 200`() {
            val notice =
                noticeRepository.save(
                    Notice(
                        title = "테스트 공지",
                        content = "테스트 내용",
                        isPinned = false,
                    ),
                )

            mockMvc
                .perform(get("/api/notices/${notice.id}"))
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.title").value("테스트 공지"))
                .andExpect(jsonPath("$.content").value("테스트 내용"))
        }

        @Test
        @DisplayName("일반 사용자가 공지사항 등록 시 403 Forbidden 반환")
        fun `create notice with normal user returns 403`() {
            val request =
                CreateNoticeRequest(
                    title = "새 공지",
                    content = "새 공지 내용",
                    isPinned = false,
                )

            mockMvc
                .perform(
                    post("/api/notices")
                        .header("Authorization", "Bearer $userToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andDo(print())
                .andExpect(status().isForbidden)
        }

        @Test
        @DisplayName("관리자가 공지사항 등록 시 201 Created 반환")
        fun `create notice with admin user returns 201`() {
            val request =
                CreateNoticeRequest(
                    title = "관리자 공지",
                    content = "관리자가 작성한 공지 내용",
                    isPinned = false,
                )

            mockMvc
                .perform(
                    post("/api/notices")
                        .header("Authorization", "Bearer $adminToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andDo(print())
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.title").value("관리자 공지"))
                .andExpect(jsonPath("$.content").value("관리자가 작성한 공지 내용"))
        }

        @Test
        @DisplayName("일반 사용자가 공지사항 수정 시 403 Forbidden 반환")
        fun `update notice with normal user returns 403`() {
            val notice =
                noticeRepository.save(
                    Notice(
                        title = "원본 공지",
                        content = "원본 내용",
                        isPinned = false,
                    ),
                )

            val requestJson = """{"title":"수정된 공지","content":"수정된 내용","isPinned":false}"""

            mockMvc
                .perform(
                    put("/api/notices/${notice.id}")
                        .header("Authorization", "Bearer $userToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson),
                ).andDo(print())
                .andExpect(status().isForbidden)
        }

        @Test
        @DisplayName("관리자가 공지사항 수정 시 200 OK 반환")
        fun `update notice with admin user returns 200`() {
            val notice =
                noticeRepository.save(
                    Notice(
                        title = "원본 공지",
                        content = "원본 내용",
                        isPinned = false,
                    ),
                )

            val requestJson = """{"title":"수정된 공지","content":"수정된 내용","isPinned":false}"""

            mockMvc
                .perform(
                    put("/api/notices/${notice.id}")
                        .header("Authorization", "Bearer $adminToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson),
                ).andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.title").value("수정된 공지"))
                .andExpect(jsonPath("$.content").value("수정된 내용"))
        }

        @Test
        @DisplayName("일반 사용자가 공지사항 삭제 시 403 Forbidden 반환")
        fun `delete notice with normal user returns 403`() {
            val notice =
                noticeRepository.save(
                    Notice(
                        title = "삭제할 공지",
                        content = "삭제할 내용",
                        isPinned = false,
                    ),
                )

            mockMvc
                .perform(
                    delete("/api/notices/${notice.id}")
                        .header("Authorization", "Bearer $userToken"),
                ).andDo(print())
                .andExpect(status().isForbidden)
        }

        @Test
        @DisplayName("관리자가 공지사항 삭제 시 204 No Content 반환")
        fun `delete notice with admin user returns 204`() {
            val notice =
                noticeRepository.save(
                    Notice(
                        title = "삭제할 공지",
                        content = "삭제할 내용",
                        isPinned = false,
                    ),
                )

            mockMvc
                .perform(
                    delete("/api/notices/${notice.id}")
                        .header("Authorization", "Bearer $adminToken"),
                ).andDo(print())
                .andExpect(status().isNoContent)
        }

        @Test
        @DisplayName("비로그인 사용자가 공지사항 등록 시 401 Unauthorized 반환")
        fun `create notice without auth returns 401`() {
            val request =
                CreateNoticeRequest(
                    title = "새 공지",
                    content = "새 공지 내용",
                    isPinned = false,
                )

            mockMvc
                .perform(
                    post("/api/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andDo(print())
                .andExpect(status().isUnauthorized)
        }
    }
