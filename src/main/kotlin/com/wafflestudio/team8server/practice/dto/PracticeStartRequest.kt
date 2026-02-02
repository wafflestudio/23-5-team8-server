package com.wafflestudio.team8server.practice.dto

data class PracticeStartRequest(
    val virtualStartTimeOption: VirtualStartTimeOption? = null,
    /**
     * 매크로 방지용 난수 오프셋 (0-999ms)
     * 프론트엔드에서 생성한 난수를 전달받아 targetTime 계산에 사용
     * null이면 0으로 처리
     */
    val randomOffsetMs: Long? = null,
)
