package com.wafflestudio.team8server.practice.controller

import com.wafflestudio.team8server.common.auth.LoggedInUserId
import com.wafflestudio.team8server.common.exception.ErrorResponse
import com.wafflestudio.team8server.course.dto.CourseDetailResponse
import com.wafflestudio.team8server.practice.config.PracticeSessionConfig
import com.wafflestudio.team8server.practice.dto.PracticeAttemptRequest
import com.wafflestudio.team8server.practice.dto.PracticeAttemptResponse
import com.wafflestudio.team8server.practice.dto.PracticeEndResponse
import com.wafflestudio.team8server.practice.dto.PracticeStartRequest
import com.wafflestudio.team8server.practice.dto.PracticeStartResponse
import com.wafflestudio.team8server.practice.service.PracticeService
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
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "수강신청 연습 API", description = "수강신청 연습 시뮬레이션 기능을 제공합니다")
@RestController
@RequestMapping("/api/practice")
class PracticeController(
    private val practiceService: PracticeService,
    private val sessionConfig: PracticeSessionConfig,
) {
    @Operation(
        summary = "연습 세션 시작",
        description =
            """
            수강신청 연습 세션을 시작합니다.
            PracticeLog를 생성하고 Redis에 세션 정보를 저장합니다(5분 TTL).
            이미 진행 중인 세션이 있으면 409 에러가 반환됩니다.

            **시작 시간 옵션:**
            - TIME_08_29_00: 08:29:00 시작 (수강신청 오픈 1분 전)
            - TIME_08_29_30: 08:29:30 시작 (수강신청 오픈 30초 전) - 기본값
            - TIME_08_29_45: 08:29:45 시작 (수강신청 오픈 15초 전)

            요청 바디를 생략하면 기본값(TIME_08_29_30)이 사용됩니다.
            """,
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "연습 세션 시작 성공",
                content = [
                    Content(
                        schema = Schema(implementation = PracticeStartResponse::class),
                        examples = [
                            ExampleObject(
                                name = "start-success",
                                summary = "세션 시작 성공",
                                value =
                                    """
                                    {
                                      "practiceLogId": 42,
                                      "virtualStartTime": "08:29:30",
                                      "targetTime": "08:30:00",
                                      "timeLimitSeconds": 300,
                                      "message": "연습 세션이 시작되었습니다. 가상 시계가 08:29:30 로 세팅되었습니다."
                                    }
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
                                summary = "인증 실패",
                                value =
                                    """
                                    {
                                      "timestamp": "2026-01-15T12:00:00",
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
            ApiResponse(
                responseCode = "409",
                description = "이미 진행 중인 세션 존재",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "active-session-exists",
                                summary = "중복 세션",
                                value =
                                    """
                                    {
                                      "timestamp": "2026-01-15T12:00:00",
                                      "status": 409,
                                      "error": "Conflict",
                                      "message": "이미 진행 중인 연습 세션이 있습니다",
                                      "errorCode": "ACTIVE_SESSION_EXISTS",
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
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "연습 세션 시작 요청 (생략 시 기본값 사용)",
        required = false,
        content = [
            Content(
                schema = Schema(implementation = PracticeStartRequest::class),
                examples = [
                    ExampleObject(
                        name = "default",
                        summary = "30초 전 시작(기본값)",
                        value = """{"virtualStartTimeOption": "TIME_08_29_30"}""",
                    ),
                    ExampleObject(
                        name = "1-minute-before",
                        summary = "1분 전 시작",
                        value = """{"virtualStartTimeOption": "TIME_08_29_00"}""",
                    ),
                    ExampleObject(
                        name = "15-seconds-before",
                        summary = "15초 전 시작",
                        value = """{"virtualStartTimeOption": "TIME_08_29_45"}""",
                    ),
                ],
            ),
        ],
    )
    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    fun startPractice(
        @Parameter(hidden = true)
        @LoggedInUserId userId: Long,
        @RequestBody(required = false) request: PracticeStartRequest?,
    ): PracticeStartResponse {
        val startTimeOption = request?.virtualStartTimeOption ?: sessionConfig.defaultStartTimeOption
        val randomOffsetMs = request?.randomOffsetMs ?: 0
        return practiceService.startPractice(userId, startTimeOption, randomOffsetMs)
    }

    @Operation(
        summary = "연습 세션 종료",
        description =
            "수강신청 연습 세션을 종료합니다. " +
                "Redis에서 세션 정보를 삭제하고, 해당 세션의 총 수강신청 시도 횟수를 반환합니다. " +
                "활성 세션이 없으면 400 에러가 반환됩니다.",
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "연습 세션 종료 성공",
                content = [
                    Content(
                        schema = Schema(implementation = PracticeEndResponse::class),
                        examples = [
                            ExampleObject(
                                name = "end-success",
                                summary = "세션 종료 성공",
                                value =
                                    """
                                    {
                                      "message": "연습 세션이 종료되었습니다.",
                                      "totalAttempts": 5
                                    }
                                    """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "활성 세션 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "no-active-session",
                                summary = "활성 세션 없음",
                                value =
                                    """
                                    {
                                      "timestamp": "2026-01-15T12:00:00",
                                      "status": 400,
                                      "error": "Bad Request",
                                      "message": "활성화된 연습 세션이 없습니다",
                                      "errorCode": "NO_ACTIVE_SESSION",
                                      "validationErrors": null
                                    }
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
                                summary = "인증 실패",
                                value =
                                    """
                                    {
                                      "timestamp": "2026-01-15T12:00:00",
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
    @PostMapping("/end")
    @ResponseStatus(HttpStatus.OK)
    fun endPractice(
        @Parameter(hidden = true)
        @LoggedInUserId userId: Long,
    ): PracticeEndResponse = practiceService.endPractice(userId)

    @Operation(
        summary = "수강신청 연습 시도",
        description =
            """
            로그정규분포를 사용하여 수강신청 성공/실패를 시뮬레이션합니다.

            **시뮬레이션 환경:**
            - 연습 시작 시간: 사용자 선택 (08:29:00, 08:29:30, 08:29:45)
            - 수강신청 오픈 시간: 08:30:00 (targetTime)
            - 연습 종료 시간: 연습 시작 시간 + 5분

            **처리 흐름:**
            1. 활성 세션 확인: Redis에 활성 세션이 있는지 확인
            2. 사용자 지연 시간 계산: 서버에서 (현재 시간 - 세션 시작 시간 - 2분) 자동 계산
            3. Early Click 처리: targetTime(08:30:00) 이전 클릭 시
               - 실패 응답 반환 (message: "수강신청 기간이 아닙니다")
               - 1초 이내 Early Click만 PracticeLog에 earlyClickDiff 기록
               - 1초 초과 Early Click은 응답만 반환 (DB 기록 안 함)
            4. Course 조회: 강의가 존재하는지 확인
            5. PreEnroll 조회: 장바구니에서 totalCompetitors(cartCount) 조회
            6. 유효 정원 계산: 수강신청 기간에 따른 effectiveQuota 계산
            7. 중복 시도 체크: 같은 세션에서 같은 강의를 이미 시도했는지 확인
            8. 로그정규분포 기반 백분위 계산: CDF를 사용하여 사용자 지연 시간의 백분위 계산
            9. 등수 산출: percentile × totalCompetitors
            10. 성공 여부 판정: rank <= capacity
            11. PracticeDetail 저장: 시도 결과와 통계 정보를 DB에 저장

            **Request 파라미터:**
            - courseId: 수강신청할 강의 ID
            - totalCompetitors: (Deprecated) 서버에서 PreEnroll.cartCount로 조회
            - capacity: (Deprecated) 서버에서 Course.effectiveQuota로 계산

            **Response:**
            - isSuccess: 수강신청 성공 여부
            - message: 결과 메시지
              - 성공: "수강신청에 성공했습니다"
              - 실패: "정원이 초과되었습니다"
              - Early Click: "수강신청 기간이 아닙니다"
              - 중복 시도: "이미 수강신청된 강의입니다" 또는 "정원이 초과되었습니다"

            **참고:**
            - 사용자 지연 시간(userLatencyMs)은 서버에서 자동으로 계산되므로 클라이언트가 보낼 필요 없음
            - 동일 강의를 여러 번 시도해도 첫 번째 결과만 유효
            """,
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "수강신청 시도 완료 (성공/실패 판정 완료)",
                content = [
                    Content(
                        schema = Schema(implementation = PracticeAttemptResponse::class),
                        examples = [
                            ExampleObject(
                                name = "attempt-success",
                                summary = "수강신청 성공",
                                value =
                                    """
                                    {
                                      "isSuccess": true,
                                      "message": "수강신청에 성공했습니다"
                                    }
                                    """,
                            ),
                            ExampleObject(
                                name = "attempt-failure",
                                summary = "정원 초과 실패",
                                value =
                                    """
                                    {
                                      "isSuccess": false,
                                      "message": "정원이 초과되었습니다"
                                    }
                                    """,
                            ),
                            ExampleObject(
                                name = "early-click",
                                summary = "Early Click (수강신청 시간 전)",
                                value =
                                    """
                                    {
                                      "isSuccess": false,
                                      "message": "수강신청 기간이 아닙니다"
                                    }
                                    """,
                            ),
                            ExampleObject(
                                name = "duplicate-attempt-success",
                                summary = "중복 시도 (이미 신청 성공한 강의)",
                                value =
                                    """
                                    {
                                      "isSuccess": true,
                                      "message": "이미 수강신청된 강의입니다"
                                    }
                                    """,
                            ),
                            ExampleObject(
                                name = "duplicate-attempt-failure",
                                summary = "중복 시도 (이미 실패한 강의)",
                                value =
                                    """
                                    {
                                      "isSuccess": false,
                                      "message": "정원이 초과되었습니다(이미 시도한 강의입니다)"
                                    }
                                    """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "세션 없음 또는 유효성 검증 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "no-session",
                                summary = "활성 세션 없음",
                                value =
                                    """
                                    {
                                      "timestamp": "2026-01-15T12:00:00",
                                      "status": 400,
                                      "error": "Bad Request",
                                      "message": "수강신청 기간이 아닙니다(연습 세션이 존재하지 않습니다)",
                                      "errorCode": "NO_ACTIVE_SESSION",
                                      "validationErrors": null
                                    }
                                    """,
                            ),
                            ExampleObject(
                                name = "validation-error",
                                summary = "유효성 검증 실패",
                                value =
                                    """
                                    {
                                      "timestamp": "2026-01-15T12:00:00",
                                      "status": 400,
                                      "error": "Bad Request",
                                      "message": "입력 값이 유효하지 않습니다",
                                      "errorCode": "VALIDATION_FAILED",
                                      "validationErrors": {
                                        "courseId": "강의 ID는 필수입니다"
                                      }
                                    }
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
                                summary = "인증 실패",
                                value =
                                    """
                                    {
                                      "timestamp": "2026-01-15T12:00:00",
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
            ApiResponse(
                responseCode = "404",
                description = "강의 또는 장바구니 항목을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "course-not-found",
                                summary = "강의 없음",
                                value =
                                    """
                                    {
                                      "timestamp": "2026-01-15T12:00:00",
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "강의를 찾을 수 없습니다 (ID: 999)",
                                      "errorCode": "RESOURCE_NOT_FOUND",
                                      "validationErrors": null
                                    }
                                    """,
                            ),
                            ExampleObject(
                                name = "pre-enroll-not-found",
                                summary = "장바구니에 해당 강의 없음",
                                value =
                                    """
                                    {
                                      "timestamp": "2026-01-15T12:00:00",
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "장바구니에 해당 강의가 없습니다",
                                      "errorCode": "RESOURCE_NOT_FOUND",
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
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "수강신청 시도 요청 정보",
        required = true,
    )
    @PostMapping("/attempt")
    @ResponseStatus(HttpStatus.OK)
    fun attemptPractice(
        @Parameter(hidden = true)
        @LoggedInUserId userId: Long,
        @Valid @RequestBody request: PracticeAttemptRequest,
    ): PracticeAttemptResponse = practiceService.attemptPractice(userId, request)

    @Operation(
        summary = "성공한 강의 목록 조회",
        description =
            """
            가장 최근 연습 세션에서 수강신청에 성공한 강의 목록을 조회합니다.

            **주요 기능:**
            - 로그인한 사용자의 가장 최근 연습 세션을 자동으로 조회합니다.
            - 해당 세션에서 isSuccess=true인 강의들만 반환합니다.
            - 강의 정보는 CourseDetailResponse 형식으로 반환됩니다.

            **Response:**
            - 성공한 강의들의 상세 정보 리스트
            - 교과목번호, 강좌번호 순으로 정렬
            """,
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "성공한 강의 목록 조회 성공",
                content = [
                    Content(
                        schema = Schema(implementation = CourseDetailResponse::class),
                        examples = [
                            ExampleObject(
                                name = "enrolled-courses-success",
                                summary = "성공한 강의 목록",
                                value =
                                    """
                                    [
                                      {
                                        "id": 123,
                                        "year": 2026,
                                        "semester": "SPRING",
                                        "classification": "전공선택",
                                        "college": "공과대학",
                                        "department": "컴퓨터공학부",
                                        "academicCourse": "학부",
                                        "academicYear": "3",
                                        "courseNumber": "4190.310",
                                        "lectureNumber": "001",
                                        "courseTitle": "운영체제",
                                        "credit": 3,
                                        "instructor": "홍길동",
                                        "placeAndTime": "{\"place\":\"301호\",\"time\":\"화(10:00~11:50)\"}",
                                        "quota": 80,
                                        "freshmanQuota": null
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
                                summary = "인증 실패",
                                value =
                                    """
                                    {
                                      "timestamp": "2026-01-20T12:00:00",
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
            ApiResponse(
                responseCode = "404",
                description = "연습 기록 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "no-practice-log",
                                summary = "연습 기록 없음",
                                value =
                                    """
                                    {
                                      "timestamp": "2026-01-20T12:00:00",
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "연습 기록이 없습니다",
                                      "errorCode": "RESOURCE_NOT_FOUND",
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
    @GetMapping("/enrolled-courses")
    @ResponseStatus(HttpStatus.OK)
    fun getEnrolledCourses(
        @Parameter(hidden = true)
        @LoggedInUserId userId: Long,
    ): List<CourseDetailResponse> = practiceService.getEnrolledCourses(userId)
}
