package com.wafflestudio.team8server.common.auth

/**
 * 현재 로그인한 사용자의 ID를 컨트롤러 메서드 파라미터로 주입받기 위한 어노테이션입니다.
 *
 * 사용 예시:
 * ```
 * @PostMapping("/practice/start")
 * fun startPractice(@LoggedInUserId userId: Long): PracticeStartResponse {
 *     return practiceService.startPractice(userId)
 * }
 * ```
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class LoggedInUserId
