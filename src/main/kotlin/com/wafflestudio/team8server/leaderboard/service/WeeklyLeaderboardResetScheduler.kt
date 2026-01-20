package com.wafflestudio.team8server.leaderboard.service

import com.wafflestudio.team8server.leaderboard.repository.WeeklyLeaderboardRecordRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class WeeklyLeaderboardResetScheduler(
    private val weeklyLeaderboardRecordRepository: WeeklyLeaderboardRecordRepository,
) {
    /**
     * 매주 월요일 00:00(Asia/Seoul)에 주간 리더보드를 초기화합니다.
     */

    @Scheduled(cron = "0 0 0 * * MON", zone = "Asia/Seoul")
    @Transactional
    fun resetWeeklyLeaderboard() {
        weeklyLeaderboardRecordRepository.deleteAllInBatch()
    }
}
