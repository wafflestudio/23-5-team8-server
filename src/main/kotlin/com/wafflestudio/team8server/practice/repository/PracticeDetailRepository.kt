package com.wafflestudio.team8server.practice.repository

import com.wafflestudio.team8server.practice.model.PracticeDetail
import com.wafflestudio.team8server.practice.model.PracticeLog
import org.springframework.data.jpa.repository.JpaRepository

interface PracticeDetailRepository : JpaRepository<PracticeDetail, Long> {
    fun countByPracticeLog(practiceLog: PracticeLog): Long

    fun findByPracticeLog(practiceLog: PracticeLog): List<PracticeDetail>
}
