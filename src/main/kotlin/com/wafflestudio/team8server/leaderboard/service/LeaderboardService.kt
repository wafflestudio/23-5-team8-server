package com.wafflestudio.team8server.leaderboard.service

import com.wafflestudio.team8server.common.exception.ResourceNotFoundException
import com.wafflestudio.team8server.leaderboard.model.LeaderboardRecord
import com.wafflestudio.team8server.leaderboard.repository.LeaderboardRecordRepository
import com.wafflestudio.team8server.practice.repository.PracticeDetailRepository
import com.wafflestudio.team8server.practice.repository.PracticeLogRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LeaderboardService(
    private val leaderboardRecordRepository: LeaderboardRecordRepository,
    private val practiceLogRepository: PracticeLogRepository,
    private val practiceDetailRepository: PracticeDetailRepository,
) {
    data class LeaderboardTopResult(
        val topFirstReactionTime: List<LeaderboardRecord>,
        val topSecondReactionTime: List<LeaderboardRecord>,
        val topCompetitionRate: List<LeaderboardRecord>,
    )

    data class MyLeaderboardResult(
        val bestFirstReactionTime: Int?,
        val bestSecondReactionTime: Int?,
        val bestCompetitionRate: Double?,
    )

    @Transactional(readOnly = true)
    fun getTopResult(limit: Int): LeaderboardTopResult {
        val safeLimit = limit.coerceIn(1, 100)
        val pageable = PageRequest.of(0, safeLimit)

        return LeaderboardTopResult(
            topFirstReactionTime = leaderboardRecordRepository.findTopByBestFirstReactionTime(pageable),
            topSecondReactionTime = leaderboardRecordRepository.findTopByBestSecondReactionTime(pageable),
            topCompetitionRate = leaderboardRecordRepository.findTopByBestCompetitionRate(pageable),
        )
    }

    @Transactional(readOnly = true)
    fun getMyResult(userId: Long): MyLeaderboardResult {
        val record = leaderboardRecordRepository.findByUserId(userId)

        return MyLeaderboardResult(
            bestFirstReactionTime = record?.bestFirstReactionTime,
            bestSecondReactionTime = record?.bestSecondReactionTime,
            bestCompetitionRate = record?.bestCompetitionRate,
        )
    }

    @Transactional
    fun updateByPracticeEnd(
        userId: Long,
        practiceLogId: Long,
    ) {
        val practiceLog =
            practiceLogRepository.findById(practiceLogId).orElseThrow {
                ResourceNotFoundException("연습 기록을 찾을 수 없습니다.")
            }

        val ownerId =
            practiceLog.user.id
                ?: throw ResourceNotFoundException("연습 기록을 찾을 수 없습니다.")
        if (ownerId != userId) {
            throw ResourceNotFoundException("연습 기록을 찾을 수 없습니다.")
        }

        val details = practiceDetailRepository.findByPracticeLogIdOrderByIdAsc(practiceLogId)

        val firstReactionCandidate = details.getOrNull(0)?.reactionTime
        val secondReactionCandidate = details.getOrNull(1)?.reactionTime

        val bestCompetitionRateCandidate =
            details
                .asSequence()
                .filter { it.isSuccess }
                .filter { it.capacity > 0 }
                .map {
                    it.totalCompetitors.toDouble() /
                        it.capacity.toDouble()
                }.maxOrNull()

        val existing = leaderboardRecordRepository.findByUserId(userId)

        val record =
            existing ?: LeaderboardRecord(
                userId = userId,
                user = practiceLog.user,
                bestFirstReactionTime = null,
                bestSecondReactionTime = null,
                bestCompetitionRate = null,
            )

        var changed = false

        if (firstReactionCandidate != null) {
            val prev = record.bestFirstReactionTime
            val shouldUpdate = prev == null || firstReactionCandidate < prev
            if (shouldUpdate) {
                record.bestFirstReactionTime = firstReactionCandidate
                changed = true
            }
        }

        if (secondReactionCandidate != null) {
            val prev = record.bestSecondReactionTime
            val shouldUpdate = prev == null || secondReactionCandidate < prev
            if (shouldUpdate) {
                record.bestSecondReactionTime = secondReactionCandidate
                changed = true
            }
        }

        if (bestCompetitionRateCandidate != null) {
            val prev = record.bestCompetitionRate
            val shouldUpdate = prev == null || bestCompetitionRateCandidate > prev
            if (shouldUpdate) {
                record.bestCompetitionRate = bestCompetitionRateCandidate
                changed = true
            }
        }

        if (changed) {
            leaderboardRecordRepository.save(record)
        }
    }
}
