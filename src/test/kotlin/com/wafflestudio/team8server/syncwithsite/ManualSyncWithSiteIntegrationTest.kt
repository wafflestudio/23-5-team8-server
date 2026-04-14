package com.wafflestudio.team8server.syncwithsite.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.wafflestudio.team8server.TestcontainersConfiguration
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class ManualSyncWithSiteIntegrationTest
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

        // @Disabled("수동 크롤링 점검용 테스트 - 평소엔 비활성화 (실행 시간 소요됨)")
        @Test
        @DisplayName("Fallback 테스트: DB 캐시가 없으면 Selenium 크롤링을 수행하여 정제된 데이터(raw 제외)를 반환해야 한다")
        fun `get sugang period fallback to real time selenium crawl when db is empty`() {
            // Crawl info data due to empty cache DB
            mockMvc
                .perform(
                    get("/api/v1/syncwithsite/sugang-period")
                        .contentType(MediaType.APPLICATION_JSON),
                ).andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.header").isString)
                .andExpect(jsonPath("$.header").isNotEmpty)
                .andExpect(jsonPath("$.body").isArray)
                .andExpect(jsonPath("$.body").isNotEmpty)
                .andExpect(jsonPath("$.body[0].category").isString)
        }

        // @Disabled("수동 크롤링 점검용 테스트 - 평소엔 비활성화 (실행 시간 소요됨)")
        @Test
        @DisplayName("동기화 즉시 실행 시 Selenium 크롤링을 수행하고 DB에 결과가 덤핑되어야 한다")
        fun `run once and check auto status with dump data`() {
            // Crawl info data due to trigger
            mockMvc
                .perform(
                    post("/api/v1/syncwithsite/run"),
                ).andDo(print())
                .andExpect(status().isAccepted)
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(jsonPath("$.startedAt").exists())

            // Check the data is pushed to DB
            mockMvc
                .perform(
                    get("/api/v1/syncwithsite/auto"),
                ).andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.lastRun").exists())
                .andExpect(jsonPath("$.lastRun.status").value("SUCCESS"))
                .andExpect(jsonPath("$.lastRun.hasDumpData").value(true))
        }
    }
