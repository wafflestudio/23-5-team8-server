package com.wafflestudio.team8server.practice.dto

import com.wafflestudio.team8server.common.dto.PageInfo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "연습 세션 목록 응답")
data class PracticeSessionListResponse(
    @Schema(description = "세션 목록")
    val items: List<PracticeSessionItem>,
    @Schema(description = "페이지 정보")
    val pageInfo: PageInfo,
)
