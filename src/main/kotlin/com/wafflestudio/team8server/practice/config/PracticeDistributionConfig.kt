package com.wafflestudio.team8server.practice.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 수강신청 연습 로그정규분포 파라미터 설정
 *
 * 교과분류(classification)별로 다른 분포 파라미터를 적용할 수 있습니다.
 * 예: 교양은 경쟁이 더 치열하므로 낮은 scale 값 사용
 */
@ConfigurationProperties(prefix = "practice.distribution")
class PracticeDistributionConfig {
    /**
     * 기본 분포 파라미터
     */
    var default: DistributionParams = DistributionParams()

    /**
     * 주어진 교과분류에 해당하는 분포 파라미터를 반환합니다.
     * 현재는 모든 분류에 대해 기본값을 반환합니다.
     * TODO: 교과분류별 차별화된 파라미터 지원
     *
     * @param classification 교과분류 (nullable)
     * @return 해당 분류의 분포 파라미터
     */
    fun getParamsForClassification(classification: String?): DistributionParams = default
}
