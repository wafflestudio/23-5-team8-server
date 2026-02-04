package com.wafflestudio.team8server.course.service

import com.wafflestudio.team8server.common.dto.PageInfo
import com.wafflestudio.team8server.course.dto.CourseDetailResponse
import com.wafflestudio.team8server.course.dto.CourseSearchRequest
import com.wafflestudio.team8server.course.dto.CourseSearchResponse
import com.wafflestudio.team8server.course.model.Course
import com.wafflestudio.team8server.course.model.Semester
import com.wafflestudio.team8server.course.repository.CourseRepository
import com.wafflestudio.team8server.course.repository.CourseSpecification
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
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
    @Value("\${course.import.batch-size:500}")
    private val courseImportBatchSize: Int,
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
                    registrationCount = course.registrationCount,
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

        val deleted = courseRepository.deleteAllByYearAndSemester(year, semester)
        if (deleted > 0) log.info("[importId={}] Deleted {} courses", importId, deleted)

        val batchSize = courseImportBatchSize.coerceIn(50, 5000)
        val buffer = ArrayList<Course>(batchSize)
        var parsedCount = 0

        val parsedTotal =
            courseExcelParser.parse(file, year, semester) { course ->
                buffer.add(course)
                parsedCount++

                if (buffer.size >= batchSize) {
                    courseRepository.saveAll(buffer)
                    courseRepository.flush()
                    entityManager.clear()
                    buffer.clear()

                    if (parsedCount % (batchSize * 5) == 0) {
                        log.info("[importId={}] parsed/inserted={}", importId, parsedCount)
                    }
                }
            }

        if (buffer.isNotEmpty()) {
            courseRepository.saveAll(buffer)
            courseRepository.flush()
            entityManager.clear()
            buffer.clear()
        }

        val elapsed = System.currentTimeMillis() - t0
        if (parsedTotal == 0) {
            log.warn("[importId={}] No courses parsed from .xlsx (elapsed={}ms)", importId, elapsed)
            return
        }

        log.info(
            "[importId={}] Imported {} courses for year={}, semester={} (elapsed={}ms)",
            importId,
            parsedTotal,
            year,
            semester,
            elapsed,
        )
    }
}
