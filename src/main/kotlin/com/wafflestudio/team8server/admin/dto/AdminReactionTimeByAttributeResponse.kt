package com.wafflestudio.team8server.admin.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "과목 속성별 반응속도 통계 항목")
data class ReactionTimeByAttributeItem(
    @Schema(description = "그룹 속성값 (e.g. '공과대학', '전공필수', 'M1234')")
    val attribute: String?,
    @Schema(description = "총 연습 시도 횟수")
    val count: Long,
    @Schema(description = "평균 반응속도 (ms)")
    val avgReactionTime: Double,
    @Schema(description = "최소 반응속도 (ms)")
    val minReactionTime: Int,
    @Schema(description = "최대 반응속도 (ms)")
    val maxReactionTime: Int,
    @Schema(description = "성공 횟수")
    val successCount: Long,
)

@Schema(description = "교과목번호별 반응속도 통계 항목")
data class ReactionTimeByCourseNumberItem(
    @Schema(description = "교과목 번호 (e.g. 'M1234')")
    val attribute: String?,
    @Schema(description = "교과목명")
    val courseName: String?,
    @Schema(description = "총 연습 시도 횟수")
    val count: Long,
    @Schema(description = "평균 반응속도 (ms)")
    val avgReactionTime: Double,
    @Schema(description = "최소 반응속도 (ms)")
    val minReactionTime: Int,
    @Schema(description = "최대 반응속도 (ms)")
    val maxReactionTime: Int,
    @Schema(description = "성공 횟수")
    val successCount: Long,
)
