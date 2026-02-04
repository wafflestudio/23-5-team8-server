package com.wafflestudio.team8server.practice.service

import com.wafflestudio.team8server.practice.repository.PracticeDetailRepository
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.math.ceil

@Service
class ReactionTimePercentileService(
    private val practiceDetailRepository: PracticeDetailRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Volatile
    private var sortedReactionTimes: List<Int> = emptyList()

    @PostConstruct
    fun loadReactionTimes() {
        refreshData()
    }

    fun refreshData() {
        sortedReactionTimes = practiceDetailRepository.findAllReactionTimesOrderByAsc()
        logger.info("반응 시간 데이터 로드 완료: ${sortedReactionTimes.size}개")
    }

    fun calculatePercentile(reactionTimeMs: Int): Double {
        if (sortedReactionTimes.isEmpty()) {
            return 0.5
        }

        val index = sortedReactionTimes.binarySearch(reactionTimeMs)
        val insertionPoint =
            if (index >= 0) {
                index
            } else {
                -(index + 1)
            }

        return insertionPoint.toDouble() / sortedReactionTimes.size
    }

    fun calculateRank(
        percentile: Double,
        totalCompetitors: Int,
    ): Int {
        val rank = percentile * totalCompetitors
        return ceil(rank).toInt().coerceAtLeast(1)
    }

    fun isSuccessful(
        rank: Int,
        capacity: Int,
    ): Boolean = rank <= capacity
}
