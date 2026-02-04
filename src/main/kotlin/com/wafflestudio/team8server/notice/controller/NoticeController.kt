package com.wafflestudio.team8server.notice.controller

import com.wafflestudio.team8server.common.auth.LoggedInUserId
import com.wafflestudio.team8server.notice.dto.CreateNoticeRequest
import com.wafflestudio.team8server.notice.dto.NoticeListResponse
import com.wafflestudio.team8server.notice.dto.NoticeResponse
import com.wafflestudio.team8server.notice.dto.UpdateNoticeRequest
import com.wafflestudio.team8server.notice.service.NoticeService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Notice", description = "공지사항 API")
@RestController
@RequestMapping("/api/notices")
class NoticeController(
    private val noticeService: NoticeService,
) {
    @Operation(summary = "공지사항 목록 조회", description = "공지사항 목록을 페이징하여 조회합니다. 고정 공지가 먼저 표시됩니다.")
    @GetMapping
    fun getNotices(
        @Parameter(description = "페이지 번호 (0부터 시작)")
        @RequestParam(defaultValue = "0")
        page: Int,
        @Parameter(description = "페이지 크기")
        @RequestParam(defaultValue = "10")
        size: Int,
    ): NoticeListResponse = noticeService.getNotices(page, size)

    @Operation(summary = "공지사항 상세 조회", description = "특정 공지사항의 상세 내용을 조회합니다.")
    @GetMapping("/{noticeId}")
    fun getNotice(
        @Parameter(description = "공지사항 ID")
        @PathVariable
        noticeId: Long,
    ): NoticeResponse = noticeService.getNotice(noticeId)

    @Operation(summary = "공지사항 등록", description = "새로운 공지사항을 등록합니다. (관리자 전용)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createNotice(
        @Parameter(hidden = true) @LoggedInUserId userId: Long,
        @Valid @RequestBody request: CreateNoticeRequest,
    ): NoticeResponse = noticeService.createNotice(request)

    @Operation(summary = "공지사항 수정", description = "기존 공지사항을 수정합니다. (관리자 전용)")
    @PutMapping("/{noticeId}")
    fun updateNotice(
        @Parameter(hidden = true) @LoggedInUserId userId: Long,
        @Parameter(description = "공지사항 ID")
        @PathVariable
        noticeId: Long,
        @Valid @RequestBody request: UpdateNoticeRequest,
    ): NoticeResponse = noticeService.updateNotice(noticeId, request)

    @Operation(summary = "공지사항 삭제", description = "공지사항을 삭제합니다. (관리자 전용)")
    @DeleteMapping("/{noticeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteNotice(
        @Parameter(hidden = true) @LoggedInUserId userId: Long,
        @Parameter(description = "공지사항 ID")
        @PathVariable
        noticeId: Long,
    ) = noticeService.deleteNotice(noticeId)
}
