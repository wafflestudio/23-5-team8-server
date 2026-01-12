package com.wafflestudio.team8server.course.service

import com.wafflestudio.team8server.course.model.Course
import com.wafflestudio.team8server.course.model.Semester
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Row
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Component
class CourseExcelParser {
    private companion object {
        const val HEADER_ROW_INDEX = 2
    }

    private val log = LoggerFactory.getLogger(CourseExcelParser::class.java)
    private val formatter = DataFormatter()

    fun parse(
        file: MultipartFile,
        year: Int,
        semester: Semester,
    ): List<Course> {
        require(!file.isEmpty) { "empty file" }

        file.inputStream.use { input ->
            HSSFWorkbook(input).use { workbook ->
                val sheet = workbook.getSheetAt(0) ?: return emptyList()
                val headerRow =
                    sheet.getRow(HEADER_ROW_INDEX)
                        ?: throw IllegalStateException("Header row not found at index $HEADER_ROW_INDEX")

                val headerIndex = buildHeaderIndex(headerRow)

                val results = mutableListOf<Course>()

                val firstDataRow = HEADER_ROW_INDEX + 1
                val lastRow = sheet.lastRowNum

                for (rowNum in firstDataRow..lastRow) {
                    val row = sheet.getRow(rowNum) ?: continue
                    if (isRowEmpty(row)) continue

                    val course =
                        parseRow(
                            row = row,
                            headerIndex = headerIndex,
                            year = year,
                            semester = semester,
                            rowNumForLog = rowNum + 1,
                        )

                    if (course != null) {
                        results += course
                    }
                }

                return results
            }
        }
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
        val quota = intCell(row, headerIndex, "정원")

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
        val instructor = stringCell(row, headerIndex, "담당교수")
        val placeAndTime = stringCell(row, headerIndex, "수업교시")
        val freshmanQuota = intCell(row, headerIndex, "신입생정원")

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
}
