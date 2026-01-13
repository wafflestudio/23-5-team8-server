package com.wafflestudio.team8server.preenroll

import com.fasterxml.jackson.databind.ObjectMapper
import com.wafflestudio.team8server.TestcontainersConfiguration
import com.wafflestudio.team8server.course.model.Course
import com.wafflestudio.team8server.course.model.Semester
import com.wafflestudio.team8server.course.repository.CourseRepository
import com.wafflestudio.team8server.preenroll.dto.PreEnrollAddRequest
import com.wafflestudio.team8server.preenroll.dto.PreEnrollUpdateCartCountRequest
import com.wafflestudio.team8server.preenroll.repository.PreEnrollRepository
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
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class PreEnrollControllerTest
    @Autowired
    constructor(
        private val webApplicationContext: WebApplicationContext,
        private val userRepository: UserRepository,
        private val localCredentialRepository: LocalCredentialRepository,
        private val courseRepository: CourseRepository,
        private val preEnrollRepository: PreEnrollRepository,
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

            preEnrollRepository.deleteAll()
            courseRepository.deleteAll()
            localCredentialRepository.deleteAll()
            userRepository.deleteAll()
        }

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

        private fun saveCourse(
            courseNumber: String,
            lectureNumber: String,
            courseTitle: String,
            placeAndTime: String?,
            quota: Int = 30,
        ): Course =
            courseRepository.save(
                Course(
                    year = 2026,
                    semester = Semester.SPRING,
                    courseNumber = courseNumber,
                    lectureNumber = lectureNumber,
                    courseTitle = courseTitle,
                    placeAndTime = placeAndTime,
                    quota = quota,
                ),
            )

        @Test
        @DisplayName("장바구니에 강의 추가 성공 시 201 반환")
        fun `add pre-enroll returns 201`() {
            val token = signupAndGetToken()

            val course =
                saveCourse(
                    courseNumber = "TEST001",
                    lectureNumber = "001",
                    courseTitle = "테스트 강의",
                    placeAndTime = """{"place":"301호","time":"수(19:00~21:50)"}""",
                    quota = 30,
                )

            val request = PreEnrollAddRequest(courseId = requireNotNull(course.id))

            mockMvc
                .perform(
                    post("/api/pre-enrolls")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andDo(print())
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.preEnrollId").isNumber)
                .andExpect(jsonPath("$.course.id").value(course.id))
                .andExpect(jsonPath("$.course.courseNumber").value("TEST001"))
                .andExpect(jsonPath("$.cartCount").value(0))
        }

        @Test
        @DisplayName("장바구니 조회 시 200 및 리스트 반환")
        fun `get pre-enrolls returns 200`() {
            val token = signupAndGetToken()

            val c1 =
                saveCourse(
                    courseNumber = "TEST001",
                    lectureNumber = "001",
                    courseTitle = "테스트 강의 1",
                    placeAndTime = """{"place":"301호","time":"수(19:00~21:50)"}""",
                    quota = 30,
                )
            val c2 =
                saveCourse(
                    courseNumber = "TEST002",
                    lectureNumber = "001",
                    courseTitle = "테스트 강의 2",
                    placeAndTime = """{"place":"302호","time":"목(18:00~19:50)"}""",
                    quota = 10,
                )

            mockMvc.perform(
                post("/api/pre-enrolls")
                    .header("Authorization", "Bearer $token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(PreEnrollAddRequest(requireNotNull(c1.id)))),
            )
            mockMvc.perform(
                post("/api/pre-enrolls")
                    .header("Authorization", "Bearer $token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(PreEnrollAddRequest(requireNotNull(c2.id)))),
            )

            mockMvc
                .perform(
                    get("/api/pre-enrolls")
                        .header("Authorization", "Bearer $token"),
                ).andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$").isArray)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].course.id").isNumber)
                .andExpect(jsonPath("$[0].course.courseNumber").isString)
                .andExpect(jsonPath("$[0].cartCount").isNumber)
        }

        @Test
        @DisplayName("overQuotaOnly=true면 cartCount > quota 인 항목만 반환")
        fun `get pre-enrolls with overQuotaOnly returns filtered items`() {
            val token = signupAndGetToken()

            val c1 =
                saveCourse(
                    courseNumber = "TEST001",
                    lectureNumber = "001",
                    courseTitle = "정원 1 강의",
                    placeAndTime = """{"place":"301호","time":"수(19:00~21:50)"}""",
                    quota = 1,
                )
            val c2 =
                saveCourse(
                    courseNumber = "TEST002",
                    lectureNumber = "001",
                    courseTitle = "정원 100 강의",
                    placeAndTime = """{"place":"302호","time":"목(18:00~19:50)"}""",
                    quota = 100,
                )

            mockMvc.perform(
                post("/api/pre-enrolls")
                    .header("Authorization", "Bearer $token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(PreEnrollAddRequest(requireNotNull(c1.id)))),
            )
            mockMvc.perform(
                post("/api/pre-enrolls")
                    .header("Authorization", "Bearer $token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(PreEnrollAddRequest(requireNotNull(c2.id)))),
            )

            mockMvc.perform(
                patch("/api/pre-enrolls/${requireNotNull(c1.id)}/cart-count")
                    .header("Authorization", "Bearer $token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(PreEnrollUpdateCartCountRequest(cartCount = 2))),
            )

            mockMvc
                .perform(
                    get("/api/pre-enrolls")
                        .header("Authorization", "Bearer $token")
                        .queryParam("overQuotaOnly", "true"),
                ).andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].course.id").value(c1.id))
        }

        @Test
        @DisplayName("장바구니에서 강의 삭제 성공 시 204 반환")
        fun `delete pre-enroll returns 204`() {
            val token = signupAndGetToken()

            val course =
                saveCourse(
                    courseNumber = "TEST001",
                    lectureNumber = "001",
                    courseTitle = "삭제 테스트 강의",
                    placeAndTime = """{"place":"301호","time":"수(19:00~21:50)"}""",
                    quota = 30,
                )

            mockMvc.perform(
                post("/api/pre-enrolls")
                    .header("Authorization", "Bearer $token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(PreEnrollAddRequest(requireNotNull(course.id)))),
            )

            mockMvc
                .perform(
                    delete("/api/pre-enrolls/${requireNotNull(course.id)}")
                        .header("Authorization", "Bearer $token"),
                ).andDo(print())
                .andExpect(status().isNoContent)

            mockMvc
                .perform(
                    get("/api/pre-enrolls")
                        .header("Authorization", "Bearer $token"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(0))
        }

        @Test
        @DisplayName("cartCount 수정 성공 시 200 반환 및 값 반영")
        fun `update cart count returns 200`() {
            val token = signupAndGetToken()

            val course =
                saveCourse(
                    courseNumber = "TEST001",
                    lectureNumber = "001",
                    courseTitle = "카운트 수정 테스트 강의",
                    placeAndTime = """{"place":"301호","time":"수(19:00~21:50)"}""",
                    quota = 30,
                )

            mockMvc.perform(
                post("/api/pre-enrolls")
                    .header("Authorization", "Bearer $token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(PreEnrollAddRequest(requireNotNull(course.id)))),
            )

            mockMvc
                .perform(
                    patch("/api/pre-enrolls/${requireNotNull(course.id)}/cart-count")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(PreEnrollUpdateCartCountRequest(cartCount = 10))),
                ).andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.course.id").value(course.id))
                .andExpect(jsonPath("$.cartCount").value(10))
        }
    }
