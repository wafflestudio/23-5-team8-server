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
        val bestFirstReactionTimeRank: Long?,
        val bestSecondReactionTime: Int?,
        val bestSecondReactionTimeRank: Long?,
        val bestCompetitionRate: Double?,
        val bestCompetitionRateRank: Long?,
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

        val first = record?.bestFirstReactionTime
        val second = record?.bestSecondReactionTime
        val rate = record?.bestCompetitionRate
        val firstRank = first?.let { leaderboardRecordRepository.countBetterFirstReactionTime(it) + 1 }
        val secondRank = second?.let { leaderboardRecordRepository.countBetterSecondReactionTime(it) + 1 }
        val rateRank = rate?.let { leaderboardRecordRepository.countBetterCompetitionRate(it) + 1 }

        return MyLeaderboardResult(
            bestFirstReactionTime = first,
            bestFirstReactionTimeRank = firstRank,
            bestSecondReactionTime = second,
            bestSecondReactionTimeRank = secondRank,
            bestCompetitionRate = rate,
            bestCompetitionRateRank = rateRank,
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
