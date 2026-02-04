package com.wafflestudio.team8server.course.sync

import com.wafflestudio.team8server.common.exception.BadRequestException
import com.wafflestudio.team8server.course.model.Semester
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

@Component
class SugangCourseExcelClient(
    private val props: CourseSyncProperties,
) {
    private val log = LoggerFactory.getLogger(SugangCourseExcelClient::class.java)

    private val httpClient: HttpClient =
        HttpClient
            .newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofMillis(props.sugang.connectTimeoutMillis))
            .build()

    fun downloadExcel(
        year: Int,
        semester: Semester,
    ): ByteArray {
        val openShtm = semesterToOpenShtm(semester)

        val form =
            linkedMapOf(
                "workType" to "EX",
                "pageNo" to "1",
                "srchOpenSchyy" to year.toString(),
                "srchOpenShtm" to openShtm,
                "srchLanguage" to props.sugang.language,
                "srchCurrPage" to "1",
                "srchPageSize" to props.sugang.pageSize.toString(),
            )

        val body = encodeForm(form)

        val request =
            HttpRequest
                .newBuilder()
                .uri(URI(props.sugang.excelUrl))
                .timeout(Duration.ofMillis(props.sugang.readTimeoutMillis))
                .header(
                    "Accept",
                    "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                ).header("Content-Type", "application/x-www-form-urlencoded")
                .header("Origin", "https://sugang.snu.ac.kr")
                .header("Referer", props.sugang.refererUrl)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36",
                ).POST(HttpRequest.BodyPublishers.ofString(body))
                .build()

        log.info(
            "Downloading sugang excel (year={}, semester={}, openShtm={})",
            year,
            semester.name,
            openShtm,
        )

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray())

        if (response.statusCode() != 200) {
            throw BadRequestException(
                "수강신청 엑셀 다운로드 실패: status=${response.statusCode()}",
            )
        }

        val headers = response.headers()
        val contentType = headers.firstValue("Content-Type").orElse("")
        val disposition = headers.firstValue("Content-Disposition").orElse("")

        if (!contentType.contains("vnd.ms-excel", ignoreCase = true)) {
            log.warn("Unexpected Content-Type: {}", contentType)
        }
        if (!disposition.contains(".xls", ignoreCase = true)) {
            log.warn("Unexpected Content-Disposition: {}", disposition)
        }

        val bytes = response.body()
        if (bytes.isEmpty()) {
            throw BadRequestException("다운로드된 엑셀 파일이 비어 있습니다.")
        }

        return bytes
    }

    private fun semesterToOpenShtm(semester: Semester): String =
        when (semester) {
            Semester.SPRING -> "U000200001U000300001"
            Semester.SUMMER -> "U000200001U000300002"
            Semester.FALL -> "U000200002U000300001"
            Semester.WINTER -> "U000200002U000300002"
        }

    private fun encodeForm(form: Map<String, String>): String =
        form.entries.joinToString("&") { (k, v) ->
            val key = URLEncoder.encode(k, StandardCharsets.UTF_8)
            val value = URLEncoder.encode(v, StandardCharsets.UTF_8)
            "$key=$value"
        }
}
