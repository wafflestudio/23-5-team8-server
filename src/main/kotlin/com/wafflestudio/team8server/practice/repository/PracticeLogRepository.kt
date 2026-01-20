package com.wafflestudio.team8server.practice.repository

import com.wafflestudio.team8server.practice.model.PracticeLog
import org.springframework.data.jpa.repository.JpaRepository

interface PracticeLogRepository : JpaRepository<PracticeLog, Long> {
    fun findFirstByUserIdOrderByPracticeAtDesc(userId: Long): PracticeLog?
}
