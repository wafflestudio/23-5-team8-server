package com.wafflestudio.team8server.admin.service

import com.wafflestudio.team8server.admin.dto.AdminDailyStatsResponse
import com.wafflestudio.team8server.admin.dto.AdminDbStatsResponse
import com.wafflestudio.team8server.practice.repository.PracticeDetailRepository
import com.wafflestudio.team8server.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class AdminService(
    private val userRepository: UserRepository,
    private val practiceDetailRepository: PracticeDetailRepository,
) {
    @Transactional(readOnly = true)
    fun getDbStats(): AdminDbStatsResponse =
        AdminDbStatsResponse(
            userCount = userRepository.count(),
            practiceDetailCount = practiceDetailRepository.count(),
        )

    @Transactional(readOnly = true)
    fun getDailyStats(
        from: LocalDate,
        to: LocalDate,
    ): AdminDailyStatsResponse =
        AdminDailyStatsResponse(
            dailyActiveUsers = emptyList(),
            dailyNewUsers = emptyList(),
            dailyPracticeDetailCounts = emptyList(),
        )
}
