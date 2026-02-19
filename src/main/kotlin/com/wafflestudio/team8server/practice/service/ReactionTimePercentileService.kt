package com.wafflestudio.team8server.practice.service

import com.wafflestudio.team8server.practice.repository.PracticeDetailRepository
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.math.ceil
import kotlin.math.pow

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

    /**
     * 경쟁률에 따라 백분위를 조정합니다.
     * 경쟁률이 낮은 과목(쉬운 과목)일수록 백분위를 낮춰서 합격을 쉽게 만듭니다.
     *
     * @param rawPercentile DB 기반 원본 백분위 (0.0 ~ 1.0)
     * @param competitionRatio 경쟁률 (총 경쟁자 수 / 정원)
     * @return 조정된 백분위 (0.0 ~ 1.0)
     */
    fun adjustPercentileByCompetition(
        rawPercentile: Double,
        competitionRatio: Double,
    ): Double {
        val exponent =
            when {
                competitionRatio < 1.5 -> {
                    // 경쟁률 1.0 ~ 1.5: 보정 적용
                    // 1.0 → exponent 3.0, 1.5 → exponent 1.0
                    7.0 - 4.0 * competitionRatio
                }
                else -> {
                    // 경쟁률 1.5 이상: 보정 없음
                    1.0
                }
            }

        return rawPercentile.pow(exponent).coerceIn(0.0, 1.0)
    }
}
