package com.wafflestudio.team8server.practice

import com.wafflestudio.team8server.practice.util.LogNormalDistributionUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class LogNormalDistributionUtilTest {
    @Test
    @DisplayName("백분위 계산 - 작은 latency는 작은 백분위를 반환")
    fun `calculatePercentile returns lower percentile for smaller latency`() {
        val util = LogNormalDistributionUtil(scale = 5.0, shape = 0.5)

        val percentile1 = util.calculatePercentile(50)
        val percentile2 = util.calculatePercentile(100)
        val percentile3 = util.calculatePercentile(200)

        // 작은 latency일수록 백분위가 작아야 함 (상위권)
        assertTrue(percentile1 < percentile2)
        assertTrue(percentile2 < percentile3)
    }

    @Test
    @DisplayName("백분위는 0과 1 사이의 값")
    fun `calculatePercentile returns value between 0 and 1`() {
        val util = LogNormalDistributionUtil(scale = 5.0, shape = 0.5)

        val percentile = util.calculatePercentile(150)

        assertTrue(percentile >= 0.0)
        assertTrue(percentile <= 1.0)
    }

    @Test
    @DisplayName("등수 계산 - 백분위 0.05, 100명 경쟁 시 5등")
    fun `calculateRank with percentile 0_05 and 100 competitors returns 5`() {
        val util = LogNormalDistributionUtil(scale = 5.0, shape = 0.5)

        val rank = util.calculateRank(percentile = 0.05, totalCompetitors = 100)

        assertEquals(5, rank)
    }

    @Test
    @DisplayName("등수 계산 - 백분위 0.50, 100명 경쟁 시 50등")
    fun `calculateRank with percentile 0_50 and 100 competitors returns 50`() {
        val util = LogNormalDistributionUtil(scale = 5.0, shape = 0.5)

        val rank = util.calculateRank(percentile = 0.50, totalCompetitors = 100)

        assertEquals(50, rank)
    }

    @Test
    @DisplayName("등수 계산 - 백분위가 매우 작아도 최소 1등")
    fun `calculateRank returns at least 1`() {
        val util = LogNormalDistributionUtil(scale = 5.0, shape = 0.5)

        val rank = util.calculateRank(percentile = 0.0, totalCompetitors = 100)

        assertEquals(1, rank)
    }

    @Test
    @DisplayName("등수와 latency로 직접 계산")
    fun `calculateRankFromLatency calculates rank directly from latency`() {
        val util = LogNormalDistributionUtil(scale = 5.0, shape = 0.5)

        val rank = util.calculateRankFromLatency(latencyMs = 150, totalCompetitors = 100)

        // 등수는 1 이상 100 이하여야 함
        assertTrue(rank >= 1)
        assertTrue(rank <= 100)
    }

    @Test
    @DisplayName("성공 판정 - 등수가 정원 이하면 성공")
    fun `isSuccessful returns true when rank is within capacity`() {
        val util = LogNormalDistributionUtil(scale = 5.0, shape = 0.5)

        assertTrue(util.isSuccessful(rank = 40, capacity = 40)) // 정원과 동일
        assertTrue(util.isSuccessful(rank = 30, capacity = 40)) // 정원 내
    }

    @Test
    @DisplayName("성공 판정 - 등수가 정원 초과면 실패")
    fun `isSuccessful returns false when rank exceeds capacity`() {
        val util = LogNormalDistributionUtil(scale = 5.0, shape = 0.5)

        val result = util.isSuccessful(rank = 41, capacity = 40) // 정원 초과

        assertEquals(false, result)
    }

    @Test
    @DisplayName("로그정규분포 파라미터에 따른 백분위 변화")
    fun `different scale and shape parameters affect percentile`() {
        val util1 = LogNormalDistributionUtil(scale = 4.0, shape = 0.3)
        val util2 = LogNormalDistributionUtil(scale = 5.0, shape = 0.5)
        val util3 = LogNormalDistributionUtil(scale = 6.0, shape = 0.7)

        val latency = 150
        val percentile1 = util1.calculatePercentile(latency)
        val percentile2 = util2.calculatePercentile(latency)
        val percentile3 = util3.calculatePercentile(latency)

        // 파라미터가 다르면 백분위도 달라야 함
        assertTrue(percentile1 != percentile2 || percentile2 != percentile3)
    }

    @Test
    @DisplayName("매우 빠른 반응 시간 (10ms) - 상위권")
    fun `very fast reaction time results in top rank`() {
        val util = LogNormalDistributionUtil(scale = 5.0, shape = 0.5)

        val rank = util.calculateRankFromLatency(latencyMs = 10, totalCompetitors = 100)

        // 10ms는 매우 빠르므로 상위권이어야 함
        assertTrue(rank <= 20)
    }

    @Test
    @DisplayName("매우 느린 반응 시간 (10000ms) - 하위권")
    fun `very slow reaction time results in low rank`() {
        val util = LogNormalDistributionUtil(scale = 5.0, shape = 0.5)

        val rank = util.calculateRankFromLatency(latencyMs = 10000, totalCompetitors = 100)

        // 10000ms는 매우 느리므로 하위권이어야 함
        assertTrue(rank >= 80)
    }
}
