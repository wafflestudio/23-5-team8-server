package com.wafflestudio.team8server.preenroll.controller

import com.wafflestudio.team8server.common.auth.LoggedInUserId
import com.wafflestudio.team8server.common.exception.ErrorResponse
import com.wafflestudio.team8server.preenroll.dto.PreEnrollAddRequest
import com.wafflestudio.team8server.preenroll.dto.PreEnrollCourseResponse
import com.wafflestudio.team8server.preenroll.service.PreEnrollService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "장바구니 API", description = "장바구니 담기, 삭제, 조회 기능을 제공합니다")
@RestController
@RequestMapping("/api/pre-enrolls")
class PreEnrollController(
    private val preEnrollService: PreEnrollService,
) {
    @Operation(
        summary = "장바구니 조회",
        description =
            """
            로그인한 사용자의 장바구니(pre-enroll) 목록을 조회합니다.

            - overQuotaOnly=true 인 경우, cartCount > quota 인 강의만 반환합니다.
            - 반환되는 객체는 Course 엔티티의 모든 필드를 포함합니다.
            """,
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "장바구니 조회 성공",
                content = [
                    Content(
                        schema = Schema(implementation = PreEnrollCourseResponse::class),
                        examples = [
                            ExampleObject(
                                name = "pre-enroll-list-success",
                                summary = "장바구니 목록 예시",
                                value =
                                    """
                                    [
                                      {
                                        "preEnrollId": 10,
                                        "course": {
                                          "id": 123,
                                          "year": 2025,
                                          "semester": "FALL",
                                          "classification": "전공선택",
                                          "college": "공과대학",
                                          "department": "컴퓨터공학부",
                                          "academicCourse": "학부",
                                          "academicYear": "3",
                                          "courseNumber": "COMP1234",
                                          "lectureNumber": "001",
                                          "courseTitle": "운영체제",
                                          "credit": 3,
                                          "instructor": "홍길동",
                                          "placeAndTime": "{\"place\":\"301호\",\"time\":\"목(18:00~19:50)/목(20:00~21:50)\"}",
                                          "quota": 30,
                                          "freshmanQuota": null
                                        },
                                        "cartCount": 35
                                      }
                                    ]
                                    """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "unauthorized",
                                summary = "인증 실패 예시",
                                value =
                                    """
                                    {
                                      "timestamp": "2026-01-08T12:00:00",
                                      "status": 401,
                                      "error": "Unauthorized",
                                      "message": "인증에 실패했습니다",
                                      "errorCode": "UNAUTHORIZED",
                                      "validationErrors": null
                                    }
                                    """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    @GetMapping
    fun getPreEnrolls(
        @LoggedInUserId userId: Long,
        @Parameter(description = "true이면 cartCount > quota 인 항목만 반환", example = "false")
        @RequestParam(required = false, defaultValue = "false")
        overQuotaOnly: Boolean,
    ): List<PreEnrollCourseResponse> = preEnrollService.getPreEnrolls(userId, overQuotaOnly)

    @Operation(
        summary = "장바구니에 강의 추가",
        description =
            """
            로그인한 사용자의 장바구니에 강의를 추가합니다.

            제약:
            - 동일 강의(courseId) 중복 담기 불가 (409)
            - 동일 교과목번호(courseNumber)인 강의가 이미 담겨 있으면 추가 불가 (409)
            - 시간대가 겹치는 강의가 이미 담겨 있으면 추가 불가 (409)
            """,
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "장바구니 추가 성공",
                content = [
                    Content(
                        schema = Schema(implementation = PreEnrollCourseResponse::class),
                        examples = [
                            ExampleObject(
                                name = "add-success",
                                summary = "추가 성공 예시",
                                value =
                                    """
                                    {
                                      "preEnrollId": 10,
                                      "course": {
                                        "id": 123,
                                        "year": 2025,
                                        "semester": "FALL",
                                        "classification": "전공선택",
                                        "college": "공과대학",
                                        "department": "컴퓨터공학부",
                                        "academicCourse": "학부",
                                        "academicYear": "3",
                                        "courseNumber": "COMP1234",
                                        "lectureNumber": "001",
                                        "courseTitle": "운영체제",
                                        "credit": 3,
                                        "instructor": "홍길동",
                                        "placeAndTime": "{\"place\":\"301호\",\"time\":\"목(18:00~19:50)/목(20:00~21:50)\"}",
                                        "quota": 30,
                                        "freshmanQuota": null
                                      },
                                      "cartCount": 0
                                    }
                                    """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "요청 형식 오류/검증 실패",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "사용자 또는 강의 없음",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "장바구니 추가 불가(중복/교과목번호 중복/시간 충돌)",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun addPreEnroll(
        @LoggedInUserId userId: Long,
        @Valid @RequestBody request: PreEnrollAddRequest,
    ): PreEnrollCourseResponse = preEnrollService.addPreEnroll(userId, request.courseId)
}
