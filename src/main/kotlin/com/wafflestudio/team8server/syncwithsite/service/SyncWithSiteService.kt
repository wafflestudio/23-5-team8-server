package com.wafflestudio.team8server.syncwithsite.service
import com.wafflestudio.team8server.common.exception.ResourceNotFoundException
import com.wafflestudio.team8server.syncwithsite.dto.SugangPeriodDto
import com.wafflestudio.team8server.syncwithsite.dto.SugangPeriodResponse
import com.wafflestudio.team8server.syncwithsite.model.SyncWithSiteRun
import com.wafflestudio.team8server.syncwithsite.model.SyncWithSiteRunStatus
import com.wafflestudio.team8server.syncwithsite.model.SyncWithSiteSetting
import com.wafflestudio.team8server.syncwithsite.repository.SyncWithSiteRunRepository
import com.wafflestudio.team8server.syncwithsite.repository.SyncWithSiteSettingRepository
import io.github.bonigarcia.wdm.WebDriverManager
import org.jsoup.Jsoup
import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean

@Service
class SyncWithSiteService(
    private val settingRepository: SyncWithSiteSettingRepository,
    private val runRepository: SyncWithSiteRunRepository,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(SyncWithSiteService::class.java)
    private val running = AtomicBoolean(false)

    fun crawlSugangPeriod(): SugangPeriodResponse {
        // Chrome driver options
        WebDriverManager.chromedriver().setup()
        val options =
            ChromeOptions().apply {
                // 시스템에 설치된 Chromium 사용 (Docker 환경)
                System.getenv("CHROME_BIN")?.let { setBinary(it) }
                addArguments("--headless=new") // background run
                addArguments("--no-sandbox")
                addArguments("--disable-dev-shm-usage")
            }

        val driver = ChromeDriver(options)

        try {
            // SNU sugang site
            driver.get("https://sugang.snu.ac.kr/sugang/co/co010.action")

            // Wait for loading dynamically-loaded tables
            val wait = WebDriverWait(driver, Duration.ofSeconds(15))
            wait.until(
                ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("div.mg-item.mg-period-guide table"),
                ),
            )

            // Parse original source html
            val pageSource =
                driver.pageSource
                    ?: throw ResourceNotFoundException("No pageSource found")
            val document = Jsoup.parse(pageSource)

            // Select header and table body both (container)
            val container =
                document.select("div.mg-item.mg-period-guide .con-box").firstOrNull()
                    ?: throw ResourceNotFoundException("Cannot found SugangPeriod container")

            // Extract h2 header
            val headerElement =
                container.select("h2").firstOrNull()
                    ?: throw ResourceNotFoundException("Cannot found SugangPeriod h2 header")

            // Change every separators to space
            // "{abcd}학년도 {n}학기 수강신청 기간안내 ※ 장바구니는 선착순이 아닙니다."
            val headerText = headerElement.text().trim()

            // Extract table
            val tableElement =
                container.select("div.table-con table").firstOrNull()
                    ?: throw IllegalStateException("Cannot found SugangPeriod table")

            // Parse table elements
            val body = mutableListOf<SugangPeriodDto>()
            val rows = tableElement.select("tbody tr")
            for (row in rows) {
                val category = row.select("th[data-th=구분]").text()
                val date = row.select("td[data-th=일자]").text()
                val time = row.select("td[data-th=시간]").text()
                val remark = row.select("td[data-th=대상]").text()

                if (category.isNotBlank() || date.isNotBlank()) {
                    body.add(
                        SugangPeriodDto(
                            category = category,
                            date = date,
                            time = time,
                            remark = remark,
                        ),
                    )
                }
            }

            return SugangPeriodResponse(
                header = headerText,
                body = body,
            )
        } finally {
            driver.quit()
        }
    }

    @Transactional
    fun enableAuto(): SyncWithSiteSetting {
        val cur = settingRepository.findById(1L).orElse(SyncWithSiteSetting(id = 1L))
        val updated = SyncWithSiteSetting(id = 1L, enabled = true, updatedAt = LocalDateTime.now())
        return settingRepository.save(updated.copyFrom(cur))
    }

    @Transactional
    fun disableAuto(): SyncWithSiteSetting {
        val cur = settingRepository.findById(1L).orElse(SyncWithSiteSetting(id = 1L))
        val updated = SyncWithSiteSetting(id = 1L, enabled = false, updatedAt = LocalDateTime.now())
        return settingRepository.save(updated.copyFrom(cur))
    }

    fun getSetting(): SyncWithSiteSetting =
        settingRepository.findById(1L).orElse(SyncWithSiteSetting(id = 1L, enabled = false, updatedAt = LocalDateTime.now()))

    fun getLastRun(): SyncWithSiteRun? = runRepository.findTopByOrderByStartedAtDesc()

    fun isEnabled(): Boolean = getSetting().enabled

    fun runOnce() {
        if (!running.compareAndSet(false, true)) {
            throw IllegalStateException("SyncWithSite is already running")
        }

        val startedAt = LocalDateTime.now()
        try {
            log.info("SyncWithSite sync started")

            // Crawl information from sugang sites
            val result = crawlSugangPeriod()

            // Serialize the values
            val dumpedJson = objectMapper.writeValueAsString(result)

            // Save results to the DB
            runRepository.save(
                SyncWithSiteRun(
                    status = SyncWithSiteRunStatus.SUCCESS,
                    startedAt = startedAt,
                    finishedAt = LocalDateTime.now(),
                    dumpedData = dumpedJson,
                    message = null,
                ),
            )
            log.info("SyncWithSite sync success")
        } catch (e: Exception) {
            // Save Error logs to the DB
            runRepository.save(
                SyncWithSiteRun(
                    status = SyncWithSiteRunStatus.FAILED,
                    startedAt = startedAt,
                    finishedAt = LocalDateTime.now(),
                    dumpedData = null,
                    message = (e.message ?: e.javaClass.simpleName).take(500),
                ),
            )
            throw e
        } finally {
            running.set(false)
        }
    }

    private fun SyncWithSiteSetting.copyFrom(prev: SyncWithSiteSetting): SyncWithSiteSetting =
        SyncWithSiteSetting(id = prev.id, enabled = this.enabled, updatedAt = this.updatedAt)

    fun getSugangPeriod(): SugangPeriodResponse {
        val lastSuccessRun = runRepository.findFirstByStatusOrderByStartedAtDesc(SyncWithSiteRunStatus.SUCCESS)

        if (lastSuccessRun != null && lastSuccessRun.dumpedData != null) {
            log.info("Returning sugang period from DB dump (runId: {})", lastSuccessRun.id)
            // Unfold serialized json data to return
            return objectMapper.readValue(lastSuccessRun.dumpedData, SugangPeriodResponse::class.java)
        }

        // Fallback logic
        log.info("No dumped data found in DB. Falling back to real-time crawling.")
        return crawlSugangPeriod()
    }
}
