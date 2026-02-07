package com.wafflestudio.team8server.admin.service

import com.wafflestudio.team8server.admin.dto.AdminDbStatsResponse
import com.wafflestudio.team8server.admin.dto.DailyCount
import com.wafflestudio.team8server.admin.dto.DailyStatsResponse
import com.wafflestudio.team8server.practice.repository.PracticeDetailRepository
import com.wafflestudio.team8server.practice.repository.PracticeLogRepository
import com.wafflestudio.team8server.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class AdminService(
    private val userRepository: UserRepository,
    private val practiceDetailRepository: PracticeDetailRepository,
    private val practiceLogRepository: PracticeLogRepository,
) {
    @Transactional(readOnly = true)
    fun getDbStats(): AdminDbStatsResponse =
        AdminDbStatsResponse(
            userCount = userRepository.count(),
            practiceDetailCount = practiceDetailRepository.count(),
        )

    @Transactional(readOnly = true)
    fun getDailyStats(startDate: LocalDate, endDate: LocalDate): DailyStatsResponse {
        val startDateTime = startDate.atStartOfDay()
        val endDateTime = endDate.plusDays(1).atStartOfDay()

        return DailyStatsResponse(
            dailySignups = userRepository.countDailySignups(startDateTime, endDateTime).toDailyCounts(),
            dailyPracticeAttempts = practiceLogRepository.countDailyPracticeAttempts(startDateTime, endDateTime).toDailyCounts(),
            dailyActiveUsers = practiceLogRepository.countDailyActiveUsers(startDateTime, endDateTime).toDailyCounts(),
        )
    }

    private fun List<Array<Any>>.toDailyCounts(): List<DailyCount> =
        map { DailyCount(date = it[0] as LocalDate, count = it[1] as Long) }
}
