package com.wafflestudio.team8server.practice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 수강신청 연습 세션 설정
 *
 * application.yml에서 설정을 주입받아 사용합니다.
 * 모든 값은 application.yml에서 반드시 설정되어야 합니다.
 */
@ConfigurationProperties(prefix = "practice.session")
class PracticeSessionConfig {
    /**
     * 연습 시작 시각 (HH:mm:ss 형식)
     * application.yml에서 설정 필수
     */
    lateinit var virtualStartTime: String

    /**
     * 수강신청 오픈 시각 (HH:mm:ss 형식)
     * application.yml에서 설정 필수
     */
    lateinit var targetTime: String

    /**
     * 연습 제한 시간 (초 단위)
     * application.yml에서 설정 필수
     */
    var timeLimitSeconds: Long = 0

    /**
     * Early click 기록 범위 (밀리초)
     * 이 값 이내로 일찍 클릭한 경우 DB에 기록됩니다.
     * 예: 1000ms이면 targetTime보다 1초 이내로 일찍 클릭한 경우 기록
     * application.yml에서 설정 필수
     */
    var earlyClickRecordingWindowMs: Int = 0

    /**
     * 분산 락 TTL (초 단위)
     * 정상 상황에서는 finally 블록에서 즉시 해제되므로 사용되지 않습니다.
     * 서버 크래시 등 비정상 상황에서 락이 영구히 걸리는 것을 방지하는 안전장치입니다.
     * application.yml에서 설정 필수
     */
    var lockTtlSeconds: Long = 0

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
