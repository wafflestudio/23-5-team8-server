package com.wafflestudio.team8server.common.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "페이지 정보")
data class PageInfo(
    @Schema(description = "현재 페이지")
    val page: Int,
    @Schema(description = "페이지 크기")
    val size: Int,
    @Schema(description = "총 요소 수")
    val totalElements: Long,
    @Schema(description = "총 페이지 수")
    val totalPages: Int,
    @Schema(description = "다음 페이지 존재 여부")
    val hasNext: Boolean,
)
