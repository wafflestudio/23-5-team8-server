package com.wafflestudio.team8server.preenroll.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "장바구니 강의 추가 요청")
data class PreEnrollAddRequest(
    @Schema(description = "장바구니에 추가할 강의 ID", example = "123")
    val courseId: Long,
)
