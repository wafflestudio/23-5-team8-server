package com.wafflestudio.team8server.syncwithsite.service
import com.wafflestudio.team8server.common.exception.ResourceNotFoundException
import com.wafflestudio.team8server.syncwithsite.dto.SugangPeriodDto
import com.wafflestudio.team8server.syncwithsite.dto.SugangPeriodResponse
import io.github.bonigarcia.wdm.WebDriverManager
import org.jsoup.Jsoup
import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class SyncWithSiteService {

    fun getSugangPeriod(): SugangPeriodResponse {
        // Chrome driver options
        WebDriverManager.chromedriver().setup()
        val options = ChromeOptions().apply {
            addArguments("--headless=new")  // background run
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
                    By.cssSelector("div.mg-item.mg-period-guide table")
                )
            )

            // Parse original source html
            val pageSource = driver.pageSource
                ?: throw ResourceNotFoundException("No pageSource found")
            val document = Jsoup.parse(pageSource)

            // Select header and table body both (container)
            val container = document.select("div.mg-item.mg-period-guide .con-box").firstOrNull()
                ?: throw ResourceNotFoundException("Cannot found SugangPeriod container")

            // Extract h2 header
            val headerElement = container.select("h2").firstOrNull()
                ?: throw ResourceNotFoundException("Cannot found SugangPeriod h2 header")

            // Change every separators to space
            // "{abcd}학년도 {n}학기 수강신청 기간안내 ※ 장바구니는 선착순이 아닙니다."
            val headerText = headerElement.text().trim()

            // Extract table
            val tableElement = container.select("div.table-con table").firstOrNull()
                ?: throw IllegalStateException("Cannot found SugangPeriod table")

            // Concatenate raw [header + table
            val rawHtml = headerElement.outerHtml() + "\n" + tableElement.outerHtml()

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
                            remark = remark
                        )
                    )
                }
            }

            return SugangPeriodResponse(
                header = headerText,
                raw = rawHtml,
                body = body
            )

        } finally {
            driver.quit()
        }
    }
}