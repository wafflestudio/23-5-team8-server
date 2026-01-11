package com.wafflestudio.team8server.practice.config

/**
 * 로그정규분포 파라미터
 */
class DistributionParams {
    /**
     * μ (mu) - 로그정규분포의 scale 파라미터
     * 값이 낮을수록 경쟁이 치열함 (빠른 반응시간 필요)
     */
    var scale: Double = 5.0

    /**
     * σ (sigma) - 로그정규분포의 shape 파라미터
     * 분포의 퍼짐 정도를 결정
     */
    var shape: Double = 0.5
}
