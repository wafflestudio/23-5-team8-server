package com.wafflestudio.team8server.practice.dto

/**
 * 연습 세션 시작 시간 옵션
 *
 * 수강신청 오픈 시간(targetTime)은 08:30:00으로 고정이며,
 * 사용자는 연습 시작 시간을 선택할 수 있습니다.
 */
enum class VirtualStartTimeOption(
    val displayTime: String,
    val offsetToTargetMs: Long,
) {
    /**
     * 08:29:00 시작 (수강신청 오픈 1분 전)
     */
    TIME_08_29_00("08:29:00", 60_000L),

    /**
     * 08:29:30 시작 (수강신청 오픈 30초 전)
     */
    TIME_08_29_30("08:29:30", 30_000L),

    /**
     * 08:29:45 시작 (수강신청 오픈 15초 전)
     */
    TIME_08_29_45("08:29:45", 15_000L),
}
