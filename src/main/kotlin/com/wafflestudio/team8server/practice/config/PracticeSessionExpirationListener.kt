package com.wafflestudio.team8server.practice.config

import com.wafflestudio.team8server.leaderboard.service.LeaderboardService
import com.wafflestudio.team8server.practice.service.PracticeSessionService
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.stereotype.Component

/**
 * Redis 키 만료 이벤트를 수신하여 연습 세션 자동 종료 시 리더보드를 갱신합니다.
 *
 * 키 형식: "practice:session:{userId}:{practiceLogId}"
 * - TTL 만료 시 이 리스너가 호출됨
 * - 수동 종료(DELETE)는 이 리스너를 트리거하지 않음
 */
@Component
class PracticeSessionExpirationListener(
    private val practiceSessionService: PracticeSessionService,
    private val leaderboardService: LeaderboardService,
) : MessageListener {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun onMessage(
        message: Message,
        pattern: ByteArray?,
    ) {
        val expiredKey = String(message.body)

        // 세션 메인 키인지 확인하고 userId, practiceLogId 추출
        val parsed = practiceSessionService.parseSessionKey(expiredKey)
        if (parsed == null) {
            // 세션 메인 키가 아님 (startTime, offset, lock 등)
            return
        }

        val (userId, practiceLogId) = parsed
        logger.info("Practice session expired - userId: $userId, practiceLogId: $practiceLogId")

        try {
            // 리더보드 갱신
            leaderboardService.updateByPracticeEnd(
                userId = userId,
                practiceLogId = practiceLogId,
            )
            leaderboardService.updateWeeklyByPracticeEnd(
                userId = userId,
                practiceLogId = practiceLogId,
            )
            logger.info("Leaderboard updated for expired session - userId: $userId, practiceLogId: $practiceLogId")
        } catch (e: Exception) {
            logger.error("Failed to update leaderboard for expired session - userId: $userId, practiceLogId: $practiceLogId", e)
        }
    }
}
