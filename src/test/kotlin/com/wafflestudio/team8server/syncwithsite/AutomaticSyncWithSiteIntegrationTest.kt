package com.wafflestudio.team8server.syncwithsite.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.wafflestudio.team8server.TestcontainersConfiguration
import com.wafflestudio.team8server.syncwithsite.model.SyncWithSiteRun
import com.wafflestudio.team8server.syncwithsite.model.SyncWithSiteRunStatus
import com.wafflestudio.team8server.syncwithsite.repository.SyncWithSiteRunRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class AutomaticSyncWithSiteIntegrationTest
    @Autowired
    constructor(
        private val webApplicationContext: WebApplicationContext,
        private val syncWithSiteRunRepository: SyncWithSiteRunRepository,
    ) {
        private lateinit var mockMvc: MockMvc
        private val objectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules()

        @BeforeEach
        fun setUp() {
            mockMvc =
                MockMvcBuilders
                    .webAppContextSetup(webApplicationContext)
                    .build()

            // Clear Previous results
            syncWithSiteRunRepository.deleteAll()
        }

        @Test
        @DisplayName("캐시 테스트: DB에 성공한 덤프 데이터가 있으면 실시간 크롤링 없이 정제된 캐시를 반환해야 한다")
        fun `get sugang period returns cached data from db`() {
            // given: Dummy Json data
            val dummyJson =
                """
                {
                    "header": "2026학년도 수강신청 안내 (DB 캐시)",
                    "body": [
                        {
                            "category": "예비수강신청",
                            "date": "2026.01.25",
                            "time": "09:00",
                            "remark": "전체 학생"
                        }
                    ]
                }
                """.trimIndent()

            syncWithSiteRunRepository.save(
                SyncWithSiteRun(
                    status = SyncWithSiteRunStatus.SUCCESS,
                    startedAt = LocalDateTime.now().minusHours(1),
                    finishedAt = LocalDateTime.now().minusMinutes(59),
                    dumpedData = dummyJson,
                ),
            )

            // when & then: Check the cached data is correctly parsed
            mockMvc
                .perform(
                    get("/api/v1/syncwithsite/sugang-period")
                        .contentType(MediaType.APPLICATION_JSON),
                ).andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.header").value("2026학년도 수강신청 안내 (DB 캐시)"))
                .andExpect(jsonPath("$.body[0].category").value("예비수강신청"))
                .andExpect(jsonPath("$.body[0].date").value("2026.01.25"))
        }

        @Test
        @DisplayName("자동 동기화 ON/OFF 상태를 변경하고 조회할 수 있어야 한다")
        fun `enable and disable auto sync`() {
            // 1. Enable (ON)
            mockMvc
                .perform(
                    post("/api/v1/syncwithsite/auto/enable"),
                ).andExpect(status().isOk)

            mockMvc
                .perform(
                    get("/api/v1/syncwithsite/auto"),
                ).andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.updatedAt").exists())

            // 2. Disable (OFF)
            mockMvc
                .perform(
                    post("/api/v1/syncwithsite/auto/disable"),
                ).andExpect(status().isOk)

            mockMvc
                .perform(
                    get("/api/v1/syncwithsite/auto"),
                ).andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.enabled").value(false))
        }
    }
