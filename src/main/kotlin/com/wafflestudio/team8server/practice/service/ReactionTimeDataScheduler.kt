package com.wafflestudio.team8server.practice.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ReactionTimeDataScheduler(
    private val reactionTimePercentileService: ReactionTimePercentileService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    fun refreshReactionTimeData() {
        logger.info("반응 시간 데이터 갱신 시작")
        reactionTimePercentileService.refreshData()
        logger.info("반응 시간 데이터 갱신 완료")
    }
}
