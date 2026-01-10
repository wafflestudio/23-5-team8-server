package com.wafflestudio.team8server.practice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 수강신청 연습 세션 설정
 *
 * application.yml에서 설정을 주입받아 사용합니다.
 * 설정되지 않은 경우 기본값이 사용됩니다.
 */
@Configuration
@ConfigurationProperties(prefix = "practice.session")
class PracticeSessionConfig {
    /**
     * 연습 시작 시각 (HH:mm:ss 형식)
     * 기본값: 08:28:00
     */
    var virtualStartTime: String = "08:28:00"

    /**
     * 수강신청 오픈 시각 (HH:mm:ss 형식)
     * 기본값: 08:30:00
     */
    var targetTime: String = "08:30:00"

    /**
     * 연습 제한 시간 (초 단위)
     * 기본값: 300초 (5분)
     */
    var timeLimitSeconds: Long = 300L

    /**
     * Early click 기록 범위 (밀리초)
     * 이 값 이내로 일찍 클릭한 경우 DB에 기록됩니다.
     * 예: 5000ms이면 targetTime보다 5초 이내로 일찍 클릭한 경우 기록
     * 기본값: 5000ms (5초)
     */
    var earlyClickRecordingWindowMs: Int = 5000

    /**
     * 분산 락 TTL (초 단위)
     * 정상 상황에서는 finally 블록에서 즉시 해제되므로 사용되지 않습니다.
     * 서버 크래시 등 비정상 상황에서 락이 영구히 걸리는 것을 방지하는 안전장치입니다.
     * 기본값: 10초
     */
    var lockTtlSeconds: Long = 10L

    // ========== Calculated Properties ==========

    /**
     * 연습 제한 시간 (밀리초)
     */
    val timeLimitMs: Long
        get() = timeLimitSeconds * 1000

    /**
     * 연습 종료 시각 (HH:mm:ss 형식)
     * virtualStartTime + timeLimitSeconds로 계산됩니다.
     */
    val timeLimit: String
        get() {
            val startTime = LocalTime.parse(virtualStartTime, DateTimeFormatter.ofPattern("HH:mm:ss"))
            val endTime = startTime.plusSeconds(timeLimitSeconds)
            return endTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        }

    /**
     * 시작 시각과 오픈 시각 간의 차이 (밀리초)
     */
    val startToTargetOffsetMs: Long
        get() {
            val start = LocalTime.parse(virtualStartTime, DateTimeFormatter.ofPattern("HH:mm:ss"))
            val target = LocalTime.parse(targetTime, DateTimeFormatter.ofPattern("HH:mm:ss"))
            return java.time.Duration
                .between(start, target)
                .toMillis()
        }
}
