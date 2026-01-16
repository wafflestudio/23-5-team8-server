package com.wafflestudio.team8server.course.controller

import com.wafflestudio.team8server.course.dto.CourseSearchRequest
import com.wafflestudio.team8server.course.dto.CourseSearchResponse
import com.wafflestudio.team8server.course.model.Semester
import com.wafflestudio.team8server.course.service.CourseService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
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
    @Operation(
        summary = "강의 검색",
        description = """
        강의 목록을 조건에 따라 검색합니다.
        
        - 모든 검색 조건은 nullable입니다.
        - page는 0부터 시작합니다.
        - size는 페이지당 item 개수입니다.
    """,
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "강의 검색 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        examples = [
                            ExampleObject(
                                name = "강의 검색 응답 예시",
                                value = """
                            {
                              "items": [
                                {
                                  "id": 123,
                                  "year": 2026,
                                  "semester": "SPRING",
                                  "classification": "전선",
                                  "college": "공과대학",
                                  "department": "컴퓨터공학부",
                                  "academicCourse": "학사",
                                  "academicYear": "2",
                                  "courseNumber": "4190.310",
                                  "lectureNumber": "001",
                                  "courseTitle": "자료구조",
                                  "credit": 3,
                                  "instructor": "홍길동",
                                  "placeAndTime": {"place": "43-1-101(무선랜제공)/43-1-201(무선랜제공)/43-1-101(무선랜제공)/43-1-201(무선랜제공)", "time": "월(11:00~12:15)/월(11:00~12:15)/수(11:00~12:15)/수(11:00~12:15)"},
                                  "quota": 60,
                                  "freshmanQuota": 20
                                }
                              ],
                              "pageInfo": {
                                "page": 0,
                                "size": 10,
                                "totalElements": 1234,
                                "totalPages": 124,
                                "hasNext": true
                              }
                            }
                            """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    @GetMapping("/search")
    fun searchCourses(
        @ParameterObject request: CourseSearchRequest,
    ): CourseSearchResponse = courseService.search(request)

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
        try {
            courseService.import(year, semester, file)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
