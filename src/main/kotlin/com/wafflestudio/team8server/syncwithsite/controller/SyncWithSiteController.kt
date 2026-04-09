package com.wafflestudio.team8server.syncwithsite.controller

import com.wafflestudio.team8server.common.exception.ErrorResponse
import com.wafflestudio.team8server.syncwithsite.dto.SugangPeriodResponse
import com.wafflestudio.team8server.syncwithsite.service.SyncWithSiteService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Sync With Site", description = "수강신청 기간 정보 연동 API")
@RestController
@RequestMapping("/api/v1/syncwithsite")
class SyncWithSiteController(
    private val syncWithSiteService: SyncWithSiteService,
) {
    @Operation(
        summary = "수강신청 기간 조회",
        description = "서울대학교 수강신청 사이트를 크롤링하여 이번 학기 수강신청 기간 정보를 가져옵니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "크롤링 및 파싱 성공",
            ),
            ApiResponse(
                responseCode = "404",
                description = "수강신청 사이트 접속 실패 또는 대상 요소를 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @GetMapping("/sugang-period")
    fun getSugangPeriod(): SugangPeriodResponse = syncWithSiteService.getSugangPeriod()
}
