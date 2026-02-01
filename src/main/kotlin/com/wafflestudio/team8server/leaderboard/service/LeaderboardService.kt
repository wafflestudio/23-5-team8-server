package com.wafflestudio.team8server.leaderboard.service

import com.wafflestudio.team8server.common.exception.BadRequestException
import com.wafflestudio.team8server.common.exception.ResourceForbiddenException
import com.wafflestudio.team8server.common.exception.ResourceNotFoundException
import com.wafflestudio.team8server.leaderboard.model.LeaderboardRecord
import com.wafflestudio.team8server.leaderboard.model.WeeklyLeaderboardRecord
import com.wafflestudio.team8server.leaderboard.repository.LeaderboardRecordRepository
import com.wafflestudio.team8server.leaderboard.repository.WeeklyLeaderboardRecordRepository
import com.wafflestudio.team8server.practice.repository.PracticeDetailRepository
import com.wafflestudio.team8server.practice.repository.PracticeLogRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class LeaderboardService(
    private val leaderboardRecordRepository: LeaderboardRecordRepository,
    private val weeklyLeaderboardRecordRepository: WeeklyLeaderboardRecordRepository,
    private val practiceLogRepository: PracticeLogRepository,
    private val practiceDetailRepository: PracticeDetailRepository,
) {
    data class LeaderboardTopResult(
        val topFirstReactionTime: Page<LeaderboardRecord>,
        val topSecondReactionTime: Page<LeaderboardRecord>,
        val topCompetitionRate: Page<LeaderboardRecord>,
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
    fun getTopResult(
        page: Int,
        size: Int,
    ): LeaderboardTopResult {
        if (page < 0) {
            throw BadRequestException("page는 0 이상이어야 합니다.")
        }
        if (size !in 1..100) {
            throw BadRequestException("size의 범위는 1-100입니다.")
        }

        val pageable = PageRequest.of(page, size)

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

    data class WeeklyLeaderboardTopResult(
        val topFirstReactionTime: Page<WeeklyLeaderboardRecord>,
        val topSecondReactionTime: Page<WeeklyLeaderboardRecord>,
        val topCompetitionRate: Page<WeeklyLeaderboardRecord>,
    )

    @Transactional(readOnly = true)
    fun getWeeklyTopResult(
        page: Int,
        size: Int,
    ): WeeklyLeaderboardTopResult {
        if (page < 0) {
            throw BadRequestException("page는 0 이상이어야 합니다.")
        }
        if (size !in 1..100) {
            throw BadRequestException("size의 범위는 1-100입니다.")
        }

        val pageable = PageRequest.of(page, size)

        return WeeklyLeaderboardTopResult(
            topFirstReactionTime = weeklyLeaderboardRecordRepository.findTopByBestFirstReactionTime(pageable),
            topSecondReactionTime = weeklyLeaderboardRecordRepository.findTopByBestSecondReactionTime(pageable),
            topCompetitionRate = weeklyLeaderboardRecordRepository.findTopByBestCompetitionRate(pageable),
        )
    }

    @Transactional(readOnly = true)
    fun getWeeklyMyResult(userId: Long): MyLeaderboardResult {
        val record = weeklyLeaderboardRecordRepository.findByUserId(userId)

        val first = record?.bestFirstReactionTime
        val second = record?.bestSecondReactionTime
        val rate = record?.bestCompetitionRate
        val firstRank = first?.let { weeklyLeaderboardRecordRepository.countBetterFirstReactionTime(it) + 1 }
        val secondRank = second?.let { weeklyLeaderboardRecordRepository.countBetterSecondReactionTime(it) + 1 }
        val rateRank = rate?.let { weeklyLeaderboardRecordRepository.countBetterCompetitionRate(it) + 1 }

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
            throw ResourceForbiddenException("본인의 연습 기록이 아닙니다.")
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
        val now = Instant.now()

        if (firstReactionCandidate != null) {
            val prev = record.bestFirstReactionTime
            val shouldUpdate = prev == null || firstReactionCandidate < prev
            if (shouldUpdate) {
                record.bestFirstReactionTime = firstReactionCandidate
                record.bestFirstReactionTimeAchievedAt = now
                changed = true
            }
        }

        if (secondReactionCandidate != null) {
            val prev = record.bestSecondReactionTime
            val shouldUpdate = prev == null || secondReactionCandidate < prev
            if (shouldUpdate) {
                record.bestSecondReactionTime = secondReactionCandidate
                record.bestSecondReactionTimeAchievedAt = now
                changed = true
            }
        }

        if (bestCompetitionRateCandidate != null) {
            val prev = record.bestCompetitionRate
            val shouldUpdate = prev == null || bestCompetitionRateCandidate > prev
            if (shouldUpdate) {
                record.bestCompetitionRate = bestCompetitionRateCandidate
                record.bestCompetitionRateAchievedAt = now
                changed = true
            }
        }

        if (changed) {
            leaderboardRecordRepository.save(record)
        }
    }

    @Transactional
    fun updateWeeklyByPracticeEnd(
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
            throw ResourceForbiddenException("본인의 연습 기록이 아닙니다.")
        }

        val details = practiceDetailRepository.findByPracticeLogIdOrderByIdAsc(practiceLogId)

        val firstReactionCandidate = details.getOrNull(0)?.reactionTime
        val secondReactionCandidate = details.getOrNull(1)?.reactionTime

        val bestCompetitionRateCandidate =
            details
                .asSequence()
                .filter { it.isSuccess }
                .filter { it.capacity > 0 }
                .map { it.totalCompetitors.toDouble() / it.capacity.toDouble() }
                .maxOrNull()

        val weekly =
            weeklyLeaderboardRecordRepository.findByUserId(userId)
                ?: WeeklyLeaderboardRecord(
                    userId = userId,
                    bestFirstReactionTime = null,
                    bestSecondReactionTime = null,
                    bestCompetitionRate = null,
                )

        var changed = false
        val now = Instant.now()

        if (firstReactionCandidate != null) {
            val prev = weekly.bestFirstReactionTime
            if (prev == null || firstReactionCandidate < prev) {
                weekly.bestFirstReactionTime = firstReactionCandidate
                weekly.bestFirstReactionTimeAchievedAt = now
                changed = true
            }
        }

        if (secondReactionCandidate != null) {
            val prev = weekly.bestSecondReactionTime
            if (prev == null || secondReactionCandidate < prev) {
                weekly.bestSecondReactionTime = secondReactionCandidate
                weekly.bestSecondReactionTimeAchievedAt = now
                changed = true
            }
        }

        if (bestCompetitionRateCandidate != null) {
            val prev = weekly.bestCompetitionRate
            if (prev == null || bestCompetitionRateCandidate > prev) {
                weekly.bestCompetitionRate = bestCompetitionRateCandidate
                weekly.bestCompetitionRateAchievedAt = now
                changed = true
            }
        }

        if (changed) {
            weeklyLeaderboardRecordRepository.save(weekly)
        }
    }
}
