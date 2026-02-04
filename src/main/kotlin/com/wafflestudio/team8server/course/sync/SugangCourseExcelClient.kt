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

    private val defaultExcelQueryParams: LinkedHashMap<String, String> =
        linkedMapOf(
            "seeMore" to "더보기",
            "srchBdNo" to "",
            "srchCamp" to "",
            "srchOpenSbjtFldCd" to "",
            "srchCptnCorsFg" to "",
            "srchCurrPage" to "1",
            "srchExcept" to "",
            "srchGenrlRemoteLtYn" to "",
            "srchIsEngSbjt" to "",
            "srchIsPendingCourse" to "",
            "srchLsnProgType" to "",
            "srchMrksApprMthdChgPosbYn" to "",
            "srchMrksGvMthd" to "",
            "srchOpenUpDeptCd" to "",
            "srchOpenMjCd" to "",
            "srchOpenPntMax" to "",
            "srchOpenPntMin" to "",
            "srchOpenSbjtDayNm" to "",
            "srchOpenSbjtNm" to "",
            "srchOpenSbjtTm" to "",
            "srchOpenSbjtTmNm" to "",
            "srchOpenShyr" to "",
            "srchOpenSubmattCorsFg" to "",
            "srchOpenSubmattFgCd1" to "",
            "srchOpenSubmattFgCd2" to "",
            "srchOpenSubmattFgCd3" to "",
            "srchOpenSubmattFgCd4" to "",
            "srchOpenSubmattFgCd5" to "",
            "srchOpenSubmattFgCd6" to "",
            "srchOpenSubmattFgCd7" to "",
            "srchOpenSubmattFgCd8" to "",
            "srchOpenSubmattFgCd9" to "",
            "srchOpenDeptCd" to "",
            "srchOpenUpSbjtFldCd" to "",
            "srchPageSize" to props.sugang.pageSize.toString(),
            "srchProfNm" to "",
            "srchSbjtCd" to "",
            "srchSbjtNm" to "",
            "srchTlsnAplyCapaCntMax" to "",
            "srchTlsnAplyCapaCntMin" to "",
            "srchTlsnRcntMax" to "",
            "srchTlsnRcntMin" to "",
            "workType" to "EX",
        )

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

        val queryParams = LinkedHashMap(defaultExcelQueryParams)
        queryParams["srchLanguage"] = props.sugang.language
        queryParams["srchOpenSchyy"] = year.toString()
        queryParams["srchOpenShtm"] = openShtm

        val requestUri = URI("${props.sugang.excelUrl}?${encodeForm(queryParams)}")

        val request =
            HttpRequest
                .newBuilder()
                .uri(requestUri)
                .timeout(Duration.ofMillis(props.sugang.readTimeoutMillis))
                .header("Accept", "*/*")
                .header("Origin", "https://sugang.snu.ac.kr")
                // 실제 브라우저에서 엑셀 다운로드 시 Referer가 엑셀 action으로 잡히는 케이스가 있음
                .header("Referer", props.sugang.excelUrl)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36",
                ).GET()
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

        // 운영 디버깅용 관측 지표 (빈 응답/HTML 응답 등)
        val contentLength = headers.firstValue("Content-Length").orElse("")
        if (contentLength.isNotBlank() && contentLength == "0") {
            log.warn("Sugang excel response has Content-Length: 0")
        }

        if (!contentType.contains("vnd.ms-excel", ignoreCase = true)) {
            log.warn("Unexpected Content-Type: {}", contentType)
        }
        if (!disposition.contains(".xls", ignoreCase = true)) {
            log.warn("Unexpected Content-Disposition: {}", disposition)
        }

        val bytes = response.body()
        if (
            bytes.isNotEmpty() &&
            !contentType.contains("vnd.ms-excel", ignoreCase = true)
        ) {
            val preview = String(bytes, 0, minOf(bytes.size, 200), StandardCharsets.UTF_8)
            log.warn("Sugang excel response preview (first {} bytes): {}", minOf(bytes.size, 200), preview)
        }
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
