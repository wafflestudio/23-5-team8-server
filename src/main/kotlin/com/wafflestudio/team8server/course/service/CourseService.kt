package com.wafflestudio.team8server.course.service

import com.wafflestudio.team8server.common.dto.PageInfo
import com.wafflestudio.team8server.course.dto.CourseDetailResponse
import com.wafflestudio.team8server.course.dto.CourseSearchRequest
import com.wafflestudio.team8server.course.dto.CourseSearchResponse
import com.wafflestudio.team8server.course.model.Semester
import com.wafflestudio.team8server.course.repository.CourseRepository
import com.wafflestudio.team8server.course.repository.CourseSpecification
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Service
class CourseService(
    private val courseRepository: CourseRepository,
    private val courseExcelParser: CourseExcelParser,
    private val entityManager: EntityManager,
) {
    private val log = LoggerFactory.getLogger(CourseExcelParser::class.java)

    fun search(request: CourseSearchRequest): CourseSearchResponse {
        val pageable =
            PageRequest.of(
                request.page,
                request.size,
                Sort.by("courseTitle").ascending(),
            )

        val specification =
            CourseSpecification.search(
                query = request.query,
                courseNumber = request.courseNumber,
                academicCourse = request.academicCourse,
                academicYear = request.academicYear,
                college = request.college,
                department = request.department,
                classification = request.classification,
            )

        val page = courseRepository.findAll(specification, pageable)

        val items =
            page.content.map { course ->
                CourseDetailResponse(
                    id = course.id!!,
                    year = course.year,
                    semester = course.semester,
                    classification = course.classification,
                    college = course.college,
                    department = course.department,
                    academicCourse = course.academicCourse,
                    academicYear = course.academicYear,
                    courseNumber = course.courseNumber,
                    lectureNumber = course.lectureNumber,
                    courseTitle = course.courseTitle,
                    credit = course.credit,
                    instructor = course.instructor,
                    placeAndTime = course.placeAndTime,
                    quota = course.quota,
                    freshmanQuota = course.freshmanQuota,
                )
            }

        return CourseSearchResponse(
            items = items,
            pageInfo =
                PageInfo(
                    page = page.number,
                    size = page.size,
                    totalElements = page.totalElements,
                    totalPages = page.totalPages,
                    hasNext = page.hasNext(),
                ),
        )
    }

    @Transactional
    fun import(
        year: Int,
        semester: Semester,
        file: MultipartFile,
    ) {
        val importId = UUID.randomUUID().toString().substring(0, 8)
        val t0 = System.currentTimeMillis()

        val parsed =
            courseExcelParser.parse(
                file = file,
                year = year,
                semester = semester,
            )
        val tParse = System.currentTimeMillis()

        val deleted = courseRepository.deleteAllByYearAndSemester(year, semester)
        val tDelete = System.currentTimeMillis()
        log.info(
            "[importId={}] parse={}ms delete={}ms deleted={}",
            importId,
            (tParse - t0),
            (tDelete - tParse),
            deleted,
        )

        if (parsed.isEmpty()) {
            log.warn("[importId={}] no courses parsed; skip insert", importId)
            return
        }

        val batchSize = 500
        var inserted = 0

        parsed.chunked(batchSize).forEachIndexed { idx, chunk ->
            courseRepository.saveAll(chunk)
            courseRepository.flush()
            entityManager.clear()
            inserted += chunk.size

            if ((idx + 1) % 5 == 0) {
                val now = System.currentTimeMillis()
                log.info("[importId={}] inserted={} elapsed={}ms", importId, inserted, (now - t0))
            }
        }

        val tEnd = System.currentTimeMillis()
        log.info("[importId={}] DONE total={}ms inserted={}", importId, (tEnd - t0), inserted)
    }
}
