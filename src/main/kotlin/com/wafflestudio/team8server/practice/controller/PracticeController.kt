package com.wafflestudio.team8server.practice.controller

import com.wafflestudio.team8server.common.auth.LoggedInUserId
import com.wafflestudio.team8server.common.exception.ErrorResponse
import com.wafflestudio.team8server.practice.dto.PracticeAttemptRequest
import com.wafflestudio.team8server.practice.dto.PracticeAttemptResponse
import com.wafflestudio.team8server.practice.dto.PracticeEndResponse
import com.wafflestudio.team8server.practice.dto.PracticeResultResponse
import com.wafflestudio.team8server.practice.dto.PracticeStartResponse
import com.wafflestudio.team8server.practice.service.PracticeService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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
) {
    @Operation(
        summary = "연습 세션 시작",
        description =
            """
            수강신청 연습 세션을 시작합니다.

            - 가상 시계가 08:28:00으로 세팅됩니다.
            - 수강신청 오픈 시간은 08:30:00입니다.
            - 연습은 08:33:00에 자동 종료됩니다 (5분 TTL).
            - 이미 진행 중인 세션이 있으면 409 에러가 반환됩니다.
            """,
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "연습 세션 시작 성공",
                content = [Content(schema = Schema(implementation = PracticeStartResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "이미 진행 중인 세션 존재",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    fun startPractice(
        @Parameter(hidden = true)
        @LoggedInUserId userId: Long,
    ): PracticeStartResponse = practiceService.startPractice(userId)

    @Operation(
        summary = "연습 세션 종료",
        description =
            """
            수강신청 연습 세션을 종료합니다.

            - Redis에서 세션이 삭제됩니다.
            - 해당 세션의 총 시도 횟수가 반환됩니다.
            - 활성 세션이 없으면 400 에러가 반환됩니다.
            """,
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "연습 세션 종료 성공",
                content = [Content(schema = Schema(implementation = PracticeEndResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "활성 세션 없음",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
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
            - 연습 시작 시간: 08:28:00
            - 수강신청 오픈 시간: 08:30:00
            - 연습 종료 시간: 08:33:00

            **주요 로직:**
            1. 시간 검증: 제한 시간 이후 요청은 실패 처리
            2. Course 조회 및 교과분류 확인
            3. 교과분류 기반 로그정규분포 파라미터 결정
               - 교양: 더 빡센 기준 (낮은 scale)
               - 기타: 기본값 사용
            4. Early Click 처리: 0ms 이하(targetTime 이전)는 실패 처리
               - earlyClickRecordingWindowMs 범위 내는 DB에 기록
            5. 로그정규분포 기반 백분위 계산
            6. 등수 산출 및 성공 여부 판정

            **파라미터 설명:**
            - courseId: 연습할 강의 ID (교과분류 정보를 포함)
            - userLatencyMs: targetTime 기준 사용자의 요청 도착 지연 시간 (ms)
              - 음수: targetTime 이전 클릭
              - 양수: targetTime 이후 클릭
              - 예: 100이면 targetTime으로부터 100ms 후
            - totalCompetitors: 전체 경쟁자 수
            - capacity: 수강 정원
            """,
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "수강신청 시도 성공 (성공/실패 판정 완료)",
                content = [Content(schema = Schema(implementation = PracticeAttemptResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "세션 없음 또는 유효성 검증 실패",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "강의를 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @PostMapping("/attempt")
    @ResponseStatus(HttpStatus.OK)
    fun attemptPractice(
        @Parameter(hidden = true)
        @LoggedInUserId userId: Long,
        @Valid @RequestBody request: PracticeAttemptRequest,
    ): PracticeAttemptResponse = practiceService.attemptPractice(userId, request)

    @Operation(
        summary = "연습 세션 결과 조회",
        description =
            """
            특정 연습 세션의 결과를 조회합니다.

            - 본인의 연습 기록만 조회할 수 있습니다.
            - 모든 시도 내역과 통계 정보를 반환합니다.
            - 마이페이지나 세션 종료 후 결과 확인에 사용됩니다.
            """,
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "결과 조회 성공",
                content = [Content(schema = Schema(implementation = PracticeResultResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "다른 사용자의 기록 접근 시도",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "연습 기록을 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @GetMapping("/results/{practiceLogId}")
    @ResponseStatus(HttpStatus.OK)
    fun getPracticeResults(
        @Parameter(hidden = true)
        @LoggedInUserId userId: Long,
        @PathVariable practiceLogId: Long,
    ): PracticeResultResponse = practiceService.getPracticeResults(userId, practiceLogId)
}
