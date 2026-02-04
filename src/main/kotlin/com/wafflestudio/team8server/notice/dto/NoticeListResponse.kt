package com.wafflestudio.team8server.notice.dto

import com.wafflestudio.team8server.common.dto.PageInfo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "공지사항 목록 응답")
data class NoticeListResponse(
    @Schema(description = "공지사항 목록")
    val items: List<NoticeResponse>,
    @Schema(description = "페이지 정보")
    val pageInfo: PageInfo,
)
