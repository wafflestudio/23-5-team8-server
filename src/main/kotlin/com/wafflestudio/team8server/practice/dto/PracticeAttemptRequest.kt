package com.wafflestudio.team8server.practice.dto

import jakarta.validation.constraints.NotNull

data class PracticeAttemptRequest(
    @field:NotNull(message = "강의 ID는 필수입니다")
    val courseId: Long,
    // TODO: 프론트 수정 후 아래 두 필드 제거
    // - totalCompetitors는 PreEnroll.cartCount에서 조회
    // - capacity는 Course.getEffectiveQuota()에서 계산
    @Deprecated("서버에서 DB 조회로 대체됨. 프론트 수정 후 제거 예정")
    val totalCompetitors: Int? = null,
    @Deprecated("서버에서 DB 조회로 대체됨. 프론트 수정 후 제거 예정")
    val capacity: Int? = null,
)
