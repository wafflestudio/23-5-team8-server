package com.wafflestudio.team8server.admin.service

import com.wafflestudio.team8server.admin.dto.AdminDailyCountItem
import com.wafflestudio.team8server.admin.dto.AdminDailyStatsResponse
import com.wafflestudio.team8server.admin.dto.AdminDbStatsResponse
import com.wafflestudio.team8server.admin.dto.AdminReactionTimeHistogramResponse
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
    fun getDailyStats(
        from: LocalDate,
        to: LocalDate,
    ): AdminDailyStatsResponse {
        val fromAt = from.atStartOfDay()
        val toExclusive = to.plusDays(1).atStartOfDay()

        val dailyActiveUsers =
            practiceLogRepository
                .countDailyActiveUsers(fromAt = fromAt, toExclusive = toExclusive)
                .map { AdminDailyCountItem(date = it.getDate(), count = it.getCount()) }

        val dailyNewUsers =
            userRepository
                .countDailyNewUsers(fromAt = fromAt, toExclusive = toExclusive)
                .map { AdminDailyCountItem(date = it.getDate(), count = it.getCount()) }

        val dailyPracticeDetailCounts =
            practiceDetailRepository
                .countDailyPracticeDetails(fromAt = fromAt, toExclusive = toExclusive)
                .map { AdminDailyCountItem(date = it.getDate(), count = it.getCount()) }

        return AdminDailyStatsResponse(
            dailyActiveUsers = dailyActiveUsers,
            dailyNewUsers = dailyNewUsers,
            dailyPracticeDetailCounts = dailyPracticeDetailCounts,
        )
    }

    companion object {
        private const val BIN_SIZE_MS = 10
        private const val MAX_MS = 30000
        private const val BIN_COUNT = MAX_MS / BIN_SIZE_MS // 3000
    }

    @Transactional(readOnly = true)
    fun getReactionTimeHistogram(): AdminReactionTimeHistogramResponse {
        val rawBins = practiceDetailRepository.countReactionTimeHistogramBins()
        val overflowCount = practiceDetailRepository.countReactionTimeOverflow()

        val bins = MutableList(BIN_COUNT) { 0L }

        for (row in rawBins) {
            val index = (row[0] as Number).toInt()
            val count = (row[1] as Number).toLong()
            if (index in 0 until BIN_COUNT) {
                bins[index] = count
            }
        }

        return AdminReactionTimeHistogramResponse(
            binSizeMs = BIN_SIZE_MS,
            maxMs = MAX_MS,
            overflowCount = overflowCount,
            bins = bins,
        )
    }
}
