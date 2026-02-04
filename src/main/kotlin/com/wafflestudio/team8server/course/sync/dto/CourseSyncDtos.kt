package com.wafflestudio.team8server.course.sync.dto

import com.wafflestudio.team8server.course.model.Semester
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "강의 자동 동기화 상태 응답")
data class CourseSyncAutoStatusResponse(
    @Schema(description = "자동 동기화 활성화 여부", example = "true")
    val enabled: Boolean,
    @Schema(description = "스케줄 실행 주기(분)", example = "120")
    val intervalMinutes: Long,
    @Schema(description = "최근 실행 정보", nullable = true)
    val lastRun: CourseSyncLastRunResponse?,
    @Schema(description = "설정 수정 시각")
    val updatedAt: LocalDateTime,
)

@Schema(description = "최근 실행 정보")
data class CourseSyncLastRunResponse(
    @Schema(description = "실행 결과", example = "SUCCESS")
    val status: String,
    @Schema(description = "시작 시각")
    val startedAt: LocalDateTime,
    @Schema(description = "종료 시각", nullable = true)
    val finishedAt: LocalDateTime?,
    @Schema(description = "실행 대상 연도", example = "2026")
    val year: Int,
    @Schema(description = "실행 대상 학기", example = "SPRING")
    val semester: Semester,
    @Schema(description = "upsert 처리한 강의 수(알 수 없으면 null)", nullable = true)
    val rowsUpserted: Int?,
    @Schema(description = "실패 메시지(성공 시 null)", nullable = true)
    val message: String?,
)

@Schema(description = "강의 동기화 즉시 실행 요청")
data class CourseSyncRunRequest(
    @Schema(description = "연도", example = "2026")
    val year: Int,
    @Schema(description = "학기", example = "SPRING")
    val semester: Semester,
)

@Schema(description = "강의 동기화 즉시 실행 응답")
data class CourseSyncRunAcceptedResponse(
    @Schema(description = "수락 여부", example = "true")
    val accepted: Boolean = true,
    @Schema(description = "실행 시작 시각")
    val startedAt: LocalDateTime,
)
