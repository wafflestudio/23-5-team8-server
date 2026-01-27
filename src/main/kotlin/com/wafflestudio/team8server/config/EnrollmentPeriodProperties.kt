package com.wafflestudio.team8server.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 수강신청 기간 설정
 *
 * application.yaml에서 enrollment-period.type으로 설정합니다.
 * - REGULAR: 재학생 수강신청 기간 (정원 = quota - freshmanQuota)
 * - FRESHMAN: 신입생 수강신청 기간 (정원 = quota - enrolledCount) - 추후 지원 예정
 */
@ConfigurationProperties(prefix = "enrollment-period")
class EnrollmentPeriodProperties {
    /**
     * 현재 수강신청 기간 타입
     * application.yaml에서 설정 필수
     */
    var type: EnrollmentPeriodType = EnrollmentPeriodType.REGULAR
}

enum class EnrollmentPeriodType {
    /** 재학생 수강신청 기간: 정원 = quota - freshmanQuota */
    REGULAR,

    /** 신입생 수강신청 기간: 정원 = quota - enrolledCount (추후 지원 예정) */
    FRESHMAN,
}
