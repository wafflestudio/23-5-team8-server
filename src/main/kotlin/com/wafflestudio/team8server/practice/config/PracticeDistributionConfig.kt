package com.wafflestudio.team8server.practice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * 수강신청 연습 로그정규분포 파라미터 설정
 *
 * 교과분류(classification)별로 다른 분포 파라미터를 적용할 수 있습니다.
 * 예: 교양은 경쟁이 더 치열하므로 낮은 scale 값 사용
 */
@Configuration
@ConfigurationProperties(prefix = "practice.distribution")
class PracticeDistributionConfig {
    /**
     * 기본 분포 파라미터
     * 교과분류가 매핑되지 않은 경우 사용됩니다.
     */
    var default: DistributionParams = DistributionParams()

    /**
     * 교과분류별 커스텀 분포 파라미터
     * key: 교과분류 (예: "교양", "전공필수", "전공선택")
     * value: 해당 분류의 분포 파라미터
     */
    var classifications: Map<String, DistributionParams> = emptyMap()

    /**
     * 주어진 교과분류에 해당하는 분포 파라미터를 반환합니다.
     * 매핑되지 않은 분류는 기본값을 반환합니다.
     *
     * @param classification 교과분류 (nullable)
     * @return 해당 분류의 분포 파라미터
     */
    fun getParamsForClassification(classification: String?): DistributionParams {
        if (classification == null) {
            return default
        }
        return classifications[classification] ?: default
    }

    /**
     * 로그정규분포 파라미터
     */
    data class DistributionParams(
        /**
         * μ (mu) - 로그정규분포의 scale 파라미터
         * 값이 낮을수록 경쟁이 치열함 (빠른 반응시간 필요)
         */
        var scale: Double = 5.0,

        /**
         * σ (sigma) - 로그정규분포의 shape 파라미터
         * 분포의 퍼짐 정도를 결정
         */
        var shape: Double = 0.5,
    )
}
