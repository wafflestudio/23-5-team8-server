package com.wafflestudio.team8server.course.controller

import com.wafflestudio.team8server.course.dto.CourseSearchRequest
import com.wafflestudio.team8server.course.dto.CourseSearchResponse
import com.wafflestudio.team8server.course.model.Semester
import com.wafflestudio.team8server.course.service.CourseService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@Tag(name = "강의 API", description = "강의 검색, 강의 목록 적재 기능을 제공합니다")
@RestController
@RequestMapping("/api/courses")
class CourseController(
    private val courseService: CourseService,
) {
    @Operation(summary = "강의 검색")
    @GetMapping("/search")
    fun searchCourses(
        @ModelAttribute request: CourseSearchRequest,
    ): CourseSearchResponse =
        courseService.search(request)

    @Operation(summary = "학기 강의 목록 파일 import")
    @PostMapping(
        "/import",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun importCourses(
        @RequestParam year: Int,
        @RequestParam semester: Semester,
        @RequestPart file: MultipartFile,
    ) {
        courseService.import(year, semester, file)
    }
}