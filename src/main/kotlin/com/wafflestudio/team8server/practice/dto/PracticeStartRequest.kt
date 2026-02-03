package com.wafflestudio.team8server.practice.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "연습 시작 요청")
data class PracticeStartRequest(
    @Schema(description = "가상 시작 시간 옵션", example = "BEFORE_30_SEC")
    val virtualStartTimeOption: VirtualStartTimeOption? = null,
    @Schema(
        description = "매크로 방지용 난수 오프셋 (0-999ms). 프론트엔드에서 생성한 난수를 전달받아 targetTime 계산에 사용. null이면 0으로 처리",
        example = "500",
        minimum = "0",
        maximum = "999",
    )
    val randomOffsetMs: Long? = null,
)
