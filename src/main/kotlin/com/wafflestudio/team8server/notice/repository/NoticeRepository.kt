package com.wafflestudio.team8server.notice.repository

import com.wafflestudio.team8server.notice.model.Notice
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface NoticeRepository : JpaRepository<Notice, Long> {
    @Query("SELECT n FROM Notice n ORDER BY n.isPinned DESC, n.createdAt DESC")
    fun findAllOrderByPinnedAndCreatedAt(pageable: Pageable): Page<Notice>
}
