package com.wafflestudio.team8server.syncwithsite.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.wafflestudio.team8server.TestcontainersConfiguration
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
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class SyncWithSiteIntegrationTest
    @Autowired
    constructor(
        private val webApplicationContext: WebApplicationContext,
    ) {
        private lateinit var mockMvc: MockMvc
        private val objectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules()

        @BeforeEach
        fun setUp() {
            mockMvc =
                MockMvcBuilders
                    .webAppContextSetup(webApplicationContext)
                    .build()
        }

        // @Disabled("수동 크롤링 점검용 테스트 - 평소엔 비활성화")
        @Test
        @DisplayName("Selenium Integration: 실제 수강신청 사이트에서 제목과 표 데이터를 긁어와야 한다")
        fun `get sugang period with selenium returns valid header and body`() {
            mockMvc
                .perform(
                    get("/api/v1/syncwithsite/sugang-period")
                        .contentType(MediaType.APPLICATION_JSON),
                ).andDo(print())
                .andExpect(status().isOk)
                // 1. header
                .andExpect(jsonPath("$.header").isString)
                .andExpect(jsonPath("$.header").isNotEmpty)
                // 2. raw HTML
                .andExpect(jsonPath("$.raw").isString)
                .andExpect(jsonPath("$.raw").isNotEmpty)
                // 3. body List
                .andExpect(jsonPath("$.body").isArray)
                .andExpect(jsonPath("$.body").isNotEmpty)
                .andExpect(jsonPath("$.body[0].category").isString)
                .andExpect(jsonPath("$.body[0].category").isNotEmpty)
                .andExpect(jsonPath("$.body[0].date").isString)
                .andExpect(jsonPath("$.body[0].time").isString)
                .andExpect(jsonPath("$.body[0].remark").isString)
        }
    }
