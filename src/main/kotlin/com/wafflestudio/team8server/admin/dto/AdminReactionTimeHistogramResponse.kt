package com.wafflestudio.team8server.admin.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "반응속도 히스토그램 응답")
data class AdminReactionTimeHistogramResponse(
    @Schema(description = "bin 크기(ms)", example = "10")
    val binSizeMs: Int,
    @Schema(description = "히스토그램 상한(ms)", example = "30000")
    val maxMs: Int,
    @Schema(description = "상한 초과 데이터 수", example = "60")
    val overflowCount: Long,
    @Schema(description = "각 bin의 count 리스트")
    val bins: List<Long>,
)
