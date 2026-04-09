package com.wafflestudio.team8server.syncwithsite.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "수강신청 기간 크롤링 응답")
data class SugangPeriodResponse(
    @Schema(
        description = "수강신청 안내 헤더 텍스트",
        example = "2026학년도 1학기 수강신청 기간안내 ※ 장바구니는 선착순이 아닙니다.",
    )
    val header: String,
    @Schema(
        description = "수강신청 안내 및 테이블 원본 HTML",
        example = "<h2>...</h2><table>...</table>",
    )
    val raw: String,
    @Schema(description = "파싱된 수강신청 기간 목록")
    val body: List<SugangPeriodDto>,
)

@Schema(description = "수강신청 기간 항목")
data class SugangPeriodDto(
    @Schema(description = "구분", example = "수강취소기간")
    val category: String,
    @Schema(description = "일자", example = "2026-04-02(목) ~ 2026-04-21(화)")
    val date: String,
    @Schema(description = "시간", example = "10:00 ~ 23:59")
    val time: String,
    @Schema(description = "대상 및 비고", example = "마감:~4.21.(화)(메뉴: mySNU-학사정보...)")
    val remark: String,
)
