package com.wafflestudio.team8server.practice.util

import org.apache.commons.math3.distribution.LogNormalDistribution
import kotlin.math.ceil

/**
 * 로그정규분포를 사용한 수강신청 성공/실패 판별 유틸리티
 *
 * 로그정규분포는 자연로그를 취했을 때 정규분포를 따르는 분포입니다.
 * ln(x) ~ N(μ, σ) 일 때, x ~ LogNormal(μ, σ)
 *
 * @param scale μ (mu) - 로그정규분포의 scale 파라미터 (정규분포의 평균)
 * @param shape σ (sigma) - 로그정규분포의 shape 파라미터 (정규분포의 표준편차)
 */
class LogNormalDistributionUtil(
    private val scale: Double,
    private val shape: Double,
) {
    private val distribution: LogNormalDistribution = LogNormalDistribution(scale, shape)

    /**
     * 주어진 latency에 대한 백분위(percentile)를 계산합니다.
     *
     * @param latencyMs 사용자의 반응 시간 (ms)
     * @return 백분위 (0.0 ~ 1.0), 예: 0.05 = 상위 5%
     */
    fun calculatePercentile(latencyMs: Int): Double {
        // CDF(Cumulative Distribution Function)를 사용하여 백분위 계산
        // CDF는 latencyMs 이하의 값이 나올 확률을 반환
        // 예: CDF(100) = 0.95 이면, 95%의 사람들이 100ms 이하로 반응했다는 의미
        // 즉, 사용자는 상위 5%에 속함
        return distribution.cumulativeProbability(latencyMs.toDouble())
    }

    /**
     * 백분위를 기반으로 등수를 계산합니다.
     *
     * @param percentile 백분위 (0.0 ~ 1.0)
     * @param totalCompetitors 전체 경쟁자 수
     * @return 예상 등수 (1등부터 시작)
     */
    fun calculateRank(
        percentile: Double,
        totalCompetitors: Int,
    ): Int {
        // percentile * totalCompetitors = 사용자보다 빠른 사람의 수
        // 예: percentile = 0.05, totalCompetitors = 100 이면
        // 5명이 사용자보다 빠름 → 사용자는 6등
        val rank = percentile * totalCompetitors
        return ceil(rank).toInt().coerceAtLeast(1)
    }

    /**
     * latency와 전체 경쟁자 수를 기반으로 등수를 한번에 계산합니다.
     *
     * @param latencyMs 사용자의 반응 시간 (ms)
     * @param totalCompetitors 전체 경쟁자 수
     * @return 예상 등수 (1등부터 시작)
     */
    fun calculateRankFromLatency(
        latencyMs: Int,
        totalCompetitors: Int,
    ): Int {
        val percentile = calculatePercentile(latencyMs)
        return calculateRank(percentile, totalCompetitors)
    }

    /**
     * 등수를 기반으로 성공 여부를 판정합니다.
     *
     * @param rank 등수
     * @param capacity 정원
     * @return 성공 여부
     */
    fun isSuccessful(
        rank: Int,
        capacity: Int,
    ): Boolean = rank <= capacity
}
