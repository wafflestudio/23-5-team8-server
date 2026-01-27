package com.wafflestudio.team8server.course.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.pjfanning.xlsx.StreamingReader
import com.wafflestudio.team8server.common.exception.BadRequestException
import com.wafflestudio.team8server.course.model.Course
import com.wafflestudio.team8server.course.model.Semester
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Row
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Component
class CourseExcelParser {
    private companion object {
        const val HEADER_ROW_INDEX = 2
        const val DEFAULT_ROW_CACHE_SIZE = 200
        const val DEFAULT_BUFFER_SIZE = 4096
    }

    private val log = LoggerFactory.getLogger(CourseExcelParser::class.java)
    private val formatter = DataFormatter()
    private val objectMapper = ObjectMapper()

    /**
     * Streaming parse
     */
    fun parse(
        file: MultipartFile,
        year: Int,
        semester: Semester,
        onCourse: (Course) -> Unit,
    ): Int {
        if (file.isEmpty) throw BadRequestException("빈 파일입니다.")

        requireRealXlsx(file)

        var count = 0

        file.inputStream.use { input ->
            StreamingReader
                .builder()
                .rowCacheSize(DEFAULT_ROW_CACHE_SIZE)
                .bufferSize(DEFAULT_BUFFER_SIZE)
                .open(input)
                .use { workbook ->
                    val sheet = workbook.getSheetAt(0) ?: return 0

                    var headerIndex: Map<String, Int>? = null
                    val rowIterator = sheet.iterator()

                    while (rowIterator.hasNext()) {
                        val row = rowIterator.next()
                        val rowNum = row.rowNum

                        if (rowNum == HEADER_ROW_INDEX) {
                            headerIndex = buildHeaderIndex(row)
                            log.info("Detected excel headers: {}", headerIndex!!.keys)
                            continue
                        }

                        if (rowNum < HEADER_ROW_INDEX + 1) continue
                        if (headerIndex == null) continue
                        if (isRowEmpty(row)) continue

                        val course =
                            parseRow(
                                row = row,
                                headerIndex = headerIndex!!,
                                year = year,
                                semester = semester,
                                rowNumForLog = rowNum + 1,
                            ) ?: continue

                        onCourse(course)
                        count++
                    }
                }
        }

        return count
    }

    private fun isXlsx(file: MultipartFile): Boolean {
        val name = file.originalFilename?.lowercase() ?: return false
        return name.endsWith(".xlsx")
    }

    private fun parseRow(
        row: Row,
        headerIndex: Map<String, Int>,
        year: Int,
        semester: Semester,
        rowNumForLog: Int,
    ): Course? {
        val courseNumber = stringCell(row, headerIndex, "교과목번호")
        val lectureNumber = stringCell(row, headerIndex, "강좌번호")
        val courseTitle = stringCell(row, headerIndex, "교과목명")
        val quotaPair = parseQuotaCell(row, headerIndex)
        val quota = quotaPair?.first
        val freshmanQuota = quotaPair?.second

        if (courseNumber.isNullOrBlank() || lectureNumber.isNullOrBlank() || courseTitle.isNullOrBlank() || quota == null) {
            log.debug(
                "Skip row {} due to missing required fields (courseNumber={}, lectureNumber={}, courseTitle={}, quota={})",
                rowNumForLog,
                courseNumber,
                lectureNumber,
                courseTitle,
                quota,
            )
            return null
        }

        val academicCourse = stringCell(row, headerIndex, "이수과정")
        val academicYear = stringCell(row, headerIndex, "학년")
        val classification = stringCell(row, headerIndex, "교과구분")
        val college = stringCell(row, headerIndex, "개설대학")
        val department = stringCell(row, headerIndex, "개설학과")
        val credit = intCell(row, headerIndex, "학점")
        val instructor = stringCell(row, headerIndex, "주담당교수")
        val placeRaw = stringCell(row, headerIndex, "강의실(동-호)(#연건, *평창)")
        val timeRaw = stringCell(row, headerIndex, "수업교시")

        val place = placeRaw?.trim()?.takeIf { it.isNotBlank() }
        val time = timeRaw?.trim()?.takeIf { it.isNotBlank() }

        val placeAndTime =
            if (place == null && time == null) {
                null
            } else {
                objectMapper.writeValueAsString(PlaceAndTimePayload(place = place, time = time))
            }

        return Course(
            year = year,
            semester = semester,
            classification = classification,
            college = college,
            department = department,
            academicCourse = academicCourse,
            academicYear = academicYear,
            courseNumber = courseNumber.trim(),
            lectureNumber = lectureNumber.trim(),
            courseTitle = courseTitle.trim(),
            credit = credit,
            instructor = instructor,
            placeAndTime = placeAndTime,
            quota = quota,
            freshmanQuota = freshmanQuota,
        )
    }

    private fun buildHeaderIndex(headerRow: Row): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        for (i in headerRow.firstCellNum.toInt()..headerRow.lastCellNum.toInt()) {
            if (i < 0) continue
            val cell = headerRow.getCell(i) ?: continue
            val name = formatter.formatCellValue(cell).trim()
            if (name.isNotEmpty()) {
                map[name] = i
            }
        }
        return map
    }

    private fun stringCell(
        row: Row,
        headerIndex: Map<String, Int>,
        header: String,
    ): String? {
        val idx = headerIndex[header] ?: return null
        val cell = row.getCell(idx) ?: return null
        val value = formatter.formatCellValue(cell).trim()
        return value.ifEmpty { null }
    }

    private fun intCell(
        row: Row,
        headerIndex: Map<String, Int>,
        header: String,
    ): Int? {
        val raw = stringCell(row, headerIndex, header) ?: return null
        val normalized = raw.removeSuffix(".0").trim()
        return normalized.toIntOrNull()
    }

    private fun isRowEmpty(row: Row): Boolean {
        val first = row.firstCellNum.toInt()
        val last = row.lastCellNum.toInt()
        if (first < 0 || last < 0) return true

        for (i in first..last) {
            val cell = row.getCell(i) ?: continue
            val value = formatter.formatCellValue(cell).trim()
            if (value.isNotEmpty()) return false
        }
        return true
    }

    private fun parseQuotaCell(
        row: Row,
        headerIndex: Map<String, Int>,
    ): Pair<Int, Int?>? {
        val raw = stringCell(row, headerIndex, "정원") ?: return null

        val trimmed = raw.trim()
        val totalMatch = Regex("""^(\d+)""").find(trimmed)
        val enrolledMatch = Regex("""\((\d+)\)""").find(trimmed)

        val total =
            totalMatch?.groupValues?.get(1)?.toIntOrNull()
                ?: return null

        val enrolled = enrolledMatch?.groupValues?.get(1)?.toIntOrNull()

        val freshmanQuota =
            if (enrolled != null) {
                total - enrolled
            } else {
                null
            }

        return total to freshmanQuota
    }

    private fun requireRealXlsx(file: MultipartFile) {
        val name = file.originalFilename?.lowercase()
        if (name == null || !name.endsWith(".xlsx")) {
            throw BadRequestException(".xlsx 파일만 지원합니다. Excel에서 '다른 이름으로 저장'으로 .xlsx로 변환해 업로드해주세요.")
        }

        val signature = readSignature(file, 8)
        if (isOle2Xls(signature)) {
            // 확장자만 xlsx로 바꾼 .xls 같은 케이스
            throw BadRequestException("업로드한 파일이 .xlsx 형식이 아닙니다(.xls로 추정). Excel에서 '다른 이름으로 저장'으로 .xlsx로 다시 저장해 업로드해주세요.")
        }

        if (!isZipXlsx(signature)) {
            // zip도 아니면 xlsx일 수 없음
            throw BadRequestException("업로드한 파일이 .xlsx 형식이 아닙니다. Excel에서 '다른 이름으로 저장'으로 .xlsx로 변환해 업로드해주세요.")
        }
    }

    private fun readSignature(
        file: MultipartFile,
        n: Int,
    ): ByteArray {
        val bytes = file.bytes // MultipartFile 구현이 스트림을 재사용 못 해도 안전
        if (bytes.isEmpty()) return ByteArray(0)
        val len = minOf(n, bytes.size)
        return bytes.copyOfRange(0, len)
    }

    private fun isZipXlsx(sig: ByteArray): Boolean {
        // ZIP: 50 4B 03 04 (local file header) or other PK variants
        return sig.size >= 2 && sig[0] == 0x50.toByte() && sig[1] == 0x4B.toByte()
    }

    private fun isOle2Xls(sig: ByteArray): Boolean {
        // OLE2: D0 CF 11 E0 A1 B1 1A E1
        val ole2 =
            byteArrayOf(
                0xD0.toByte(),
                0xCF.toByte(),
                0x11.toByte(),
                0xE0.toByte(),
                0xA1.toByte(),
                0xB1.toByte(),
                0x1A.toByte(),
                0xE1.toByte(),
            )
        if (sig.size < ole2.size) return false
        return ole2.indices.all { i -> sig[i] == ole2[i] }
    }
}

data class PlaceAndTimePayload(
    val place: String?,
    val time: String?,
)
