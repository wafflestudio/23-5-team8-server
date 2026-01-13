package com.wafflestudio.team8server.course

import com.fasterxml.jackson.databind.ObjectMapper
import com.wafflestudio.team8server.TestcontainersConfiguration
import com.wafflestudio.team8server.course.model.Course
import com.wafflestudio.team8server.course.model.Semester
import com.wafflestudio.team8server.course.repository.CourseRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class CourseControllerTest
    @Autowired
    constructor(
        private val webApplicationContext: WebApplicationContext,
        private val courseRepository: CourseRepository,
    ) {
        private lateinit var mockMvc: MockMvc
        private val objectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules()

        @BeforeEach
        fun setUp() {
            mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
            courseRepository.deleteAll()
        }

        @Test
        @DisplayName("query로 강의 검색 시 교과목명 또는 교수명에 query를 포함하는 items와 pageInfo 반환")
        fun `search courses by query returns 200 with items`() {
            courseRepository.saveAll(
                listOf(
                    Course(
                        year = 2026,
                        semester = Semester.SPRING,
                        classification = "전선",
                        college = "공과대학",
                        department = "전기·정보공학부",
                        academicCourse = "학부",
                        academicYear = "3",
                        courseNumber = "430.322",
                        lectureNumber = "001",
                        courseTitle = "컴퓨터조직론",
                        credit = 3,
                        instructor = "김장우",
                        placeAndTime = """{"place":"301-102(무선랜제공)/301-102(무선랜제공)","time":"화(17:00~18:15)/목(17:00~18:15)"}""",
                        quota = 100,
                        freshmanQuota = 0,
                    ),
                    Course(
                        year = 2026,
                        semester = Semester.SPRING,
                        classification = "전선",
                        college = "공과대학",
                        department = "전기·정보공학부",
                        academicCourse = "학부",
                        academicYear = "4",
                        courseNumber = "430.318",
                        lectureNumber = "001",
                        courseTitle = "운영체제의 기초",
                        credit = 3,
                        instructor = "김조직",
                        placeAndTime = """{"place":"301-102(무선랜제공)/301-102(무선랜제공)","time":"화(17:00~18:15)/목(17:00~18:15)"}""",
                        quota = 100,
                        freshmanQuota = 0,
                    ),
                    Course(
                        year = 2026,
                        semester = Semester.SPRING,
                        classification = "교양",
                        college = "자연과학대학",
                        department = "생명과학부",
                        academicCourse = "학부",
                        academicYear = "1",
                        courseNumber = "F35.103L",
                        lectureNumber = "001",
                        courseTitle = "생물학실험",
                        credit = 1,
                        instructor = null,
                        placeAndTime = null,
                        quota = 20,
                        freshmanQuota = 14,
                    ),
                ),
            )

            mockMvc
                .perform(
                    get("/api/courses/search")
                        .queryParam("query", "조직"),
                ).andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.items").isArray)
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].id").isNumber)
                .andExpect(jsonPath("$.items[0].courseNumber").value("430.318"))
                .andExpect(jsonPath("$.items[0].lectureNumber").value("001"))
                .andExpect(jsonPath("$.items[0].courseTitle").value("운영체제의 기초"))
                .andExpect(jsonPath("$.items[0].credit").value(3))
                .andExpect(jsonPath("$.items[0].placeAndTime").isString)
                .andExpect(jsonPath("$.items[1].id").isNumber)
                .andExpect(jsonPath("$.items[1].courseNumber").value("430.322"))
                .andExpect(jsonPath("$.items[1].lectureNumber").value("001"))
                .andExpect(jsonPath("$.items[1].courseTitle").value("컴퓨터조직론"))
                .andExpect(jsonPath("$.items[1].credit").value(3))
                .andExpect(jsonPath("$.items[1].placeAndTime").isString)
                .andExpect(jsonPath("$.pageInfo").exists())
        }

        @Test
        @DisplayName("교과목번호로 강의 검색 시 정확히 매칭되는 강의 반환")
        fun `search courses by courseNumber returns 200`() {
            courseRepository.save(
                Course(
                    year = 2026,
                    semester = Semester.SPRING,
                    classification = "전선",
                    college = "공과대학",
                    department = "전기·정보공학부",
                    academicCourse = "학부",
                    academicYear = "3",
                    courseNumber = "430.314",
                    lectureNumber = "002",
                    courseTitle = "확률변수 및 확률과정의 기초",
                    credit = 3,
                    instructor = "최완",
                    placeAndTime = """{"place":"301-102(무선랜제공)/301-102(무선랜제공)","time":"화(11:00~12:15)/목(11:00~12:15)"}""",
                    quota = 80,
                    freshmanQuota = 80,
                ),
            )

            mockMvc
                .perform(
                    get("/api/courses/search")
                        .queryParam("courseNumber", "430.314"),
                ).andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].courseTitle").value("확률변수 및 확률과정의 기초"))
                .andExpect(jsonPath("$.pageInfo").exists())
        }

        @Test
        @DisplayName("페이지네이션 적용")
        fun `search courses pagination works`() {
            courseRepository.saveAll(
                (1..3).map { idx ->
                    Course(
                        year = 2026,
                        semester = Semester.SPRING,
                        classification = "전선",
                        college = "공과대학",
                        department = "전기·정보공학부",
                        academicCourse = "학부",
                        academicYear = "3",
                        courseNumber = "001.000$idx",
                        lectureNumber = "00$idx",
                        courseTitle = "테스트$idx",
                        credit = 3,
                        instructor = "교수$idx",
                        placeAndTime = null,
                        quota = 80,
                        freshmanQuota = null,
                    )
                },
            )

            mockMvc
                .perform(
                    get("/api/courses/search")
                        .queryParam("query", "테스트")
                        .queryParam("page", "0")
                        .queryParam("size", "1"),
                ).andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.pageInfo").exists())
        }
    }
