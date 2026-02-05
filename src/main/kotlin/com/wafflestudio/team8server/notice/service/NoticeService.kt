package com.wafflestudio.team8server.notice.service

import com.wafflestudio.team8server.common.dto.PageInfo
import com.wafflestudio.team8server.common.exception.ResourceNotFoundException
import com.wafflestudio.team8server.notice.dto.CreateNoticeRequest
import com.wafflestudio.team8server.notice.dto.NoticeListResponse
import com.wafflestudio.team8server.notice.dto.NoticeResponse
import com.wafflestudio.team8server.notice.dto.UpdateNoticeRequest
import com.wafflestudio.team8server.notice.model.Notice
import com.wafflestudio.team8server.notice.repository.NoticeRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NoticeService(
    private val noticeRepository: NoticeRepository,
) {
    @Transactional(readOnly = true)
    fun getNotices(
        page: Int,
        size: Int,
    ): NoticeListResponse {
        val pageable = PageRequest.of(page, size)
        val noticePage = noticeRepository.findAllOrderByPinnedAndCreatedAt(pageable)

        return NoticeListResponse(
            items = noticePage.content.map { NoticeResponse.from(it) },
            pageInfo =
                PageInfo(
                    page = noticePage.number,
                    size = noticePage.size,
                    totalElements = noticePage.totalElements,
                    totalPages = noticePage.totalPages,
                    hasNext = noticePage.hasNext(),
                ),
        )
    }

    @Transactional(readOnly = true)
    fun getNotice(noticeId: Long): NoticeResponse {
        val notice = findNoticeById(noticeId)
        return NoticeResponse.from(notice)
    }

    @Transactional
    fun createNotice(request: CreateNoticeRequest): NoticeResponse {
        val notice =
            Notice(
                title = request.title,
                content = request.content,
                isPinned = request.isPinned,
            )
        val savedNotice = noticeRepository.save(notice)
        return NoticeResponse.from(savedNotice)
    }

    @Transactional
    fun updateNotice(
        noticeId: Long,
        request: UpdateNoticeRequest,
    ): NoticeResponse {
        val notice = findNoticeById(noticeId)
        notice.title = request.title
        notice.content = request.content
        notice.isPinned = request.isPinned
        return NoticeResponse.from(notice)
    }

    @Transactional
    fun deleteNotice(noticeId: Long) {
        val notice = findNoticeById(noticeId)
        noticeRepository.delete(notice)
    }

    private fun findNoticeById(noticeId: Long): Notice =
        noticeRepository.findById(noticeId).orElseThrow {
            ResourceNotFoundException("공지사항을 찾을 수 없습니다: $noticeId")
        }
}
