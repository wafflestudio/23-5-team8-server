package com.wafflestudio.team8server.notice.dto

import com.wafflestudio.team8server.notice.model.Notice
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "공지사항 응답")
data class NoticeResponse(
    @Schema(description = "공지사항 ID")
    val id: Long,
    @Schema(description = "공지사항 제목")
    val title: String,
    @Schema(description = "공지사항 내용")
    val content: String,
    @Schema(description = "고정 공지 여부")
    val isPinned: Boolean,
    @Schema(description = "생성일시")
    val createdAt: LocalDateTime,
    @Schema(description = "수정일시")
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(notice: Notice): NoticeResponse =
            NoticeResponse(
                id = notice.id!!,
                title = notice.title,
                content = notice.content,
                isPinned = notice.isPinned,
                createdAt = notice.createdAt,
                updatedAt = notice.updatedAt,
            )
    }
}
