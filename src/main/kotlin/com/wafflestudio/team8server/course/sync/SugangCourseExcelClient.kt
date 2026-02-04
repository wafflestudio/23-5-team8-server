package com.wafflestudio.team8server.course.sync

import com.wafflestudio.team8server.common.exception.BadRequestException
import com.wafflestudio.team8server.course.model.Semester
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.http.RequestEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.net.URI

@Component
class SugangCourseExcelClient(
    private val props: CourseSyncProperties,
) {
    private val log = LoggerFactory.getLogger(SugangCourseExcelClient::class.java)
    private val restTemplate = RestTemplate()

    fun downloadExcel(
        year: Int,
        semester: Semester,
    ): ByteArray {
        val template =
            props.sugang.downloadUrlTemplate?.takeIf { it.isNotBlank() }
                ?: throw BadRequestException("courseSync.sugang.downloadUrlTemplate 이 설정되지 않았습니다.")

        val url =
            template
                .replace("{year}", year.toString())
                .replace("{semester}", semester.name)

        log.info("Downloading sugang excel: {}", url)

        val req = RequestEntity<Any>(HttpMethod.GET, URI(url))
        val resp = restTemplate.exchange(req, ByteArray::class.java)

        if (!resp.statusCode.is2xxSuccessful) {
            throw BadRequestException("수강신청 사이트 엑셀 다운로드에 실패했습니다. status=${resp.statusCode}")
        }

        return resp.body ?: throw BadRequestException("다운로드한 엑셀 파일이 비어 있습니다.")
    }
}
