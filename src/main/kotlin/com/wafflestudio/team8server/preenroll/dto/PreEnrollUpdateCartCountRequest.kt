package com.wafflestudio.team8server.preenroll.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min

@Schema(description = "장바구니 항목의 cartCount 수정 요청")
data class PreEnrollUpdateCartCountRequest(
    @Schema(description = "cartCount 값(0 이상)", example = "10")
    @field:Min(value = 0, message = "cartCount는 0 이상이어야 합니다")
    val cartCount: Int,
)
