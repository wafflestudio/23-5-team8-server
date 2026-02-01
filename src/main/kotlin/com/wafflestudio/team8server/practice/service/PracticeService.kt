package com.wafflestudio.team8server.practice.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.wafflestudio.team8server.common.exception.ActiveSessionExistsException
import com.wafflestudio.team8server.common.exception.NoActiveSessionException
import com.wafflestudio.team8server.common.exception.ResourceNotFoundException
import com.wafflestudio.team8server.common.exception.UnauthorizedException
import com.wafflestudio.team8server.common.extension.ensureNotNull
import com.wafflestudio.team8server.common.time.TimeProvider
import com.wafflestudio.team8server.config.EnrollmentPeriodProperties
import com.wafflestudio.team8server.course.dto.CourseDetailResponse
import com.wafflestudio.team8server.course.model.getEffectiveQuota
import com.wafflestudio.team8server.course.repository.CourseRepository
import com.wafflestudio.team8server.leaderboard.service.LeaderboardService
import com.wafflestudio.team8server.practice.config.PracticeSessionConfig
import com.wafflestudio.team8server.practice.dto.PracticeAttemptRequest
import com.wafflestudio.team8server.practice.dto.PracticeAttemptResponse
import com.wafflestudio.team8server.practice.dto.PracticeAttemptResult
import com.wafflestudio.team8server.practice.dto.PracticeEndResponse
import com.wafflestudio.team8server.practice.dto.PracticeResultResponse
import com.wafflestudio.team8server.practice.dto.PracticeStartResponse
import com.wafflestudio.team8server.practice.dto.VirtualStartTimeOption
import com.wafflestudio.team8server.practice.model.PracticeDetail
import com.wafflestudio.team8server.practice.model.PracticeLog
import com.wafflestudio.team8server.practice.repository.PracticeDetailRepository
import com.wafflestudio.team8server.practice.repository.PracticeLogRepository
import com.wafflestudio.team8server.preenroll.repository.PreEnrollRepository
import com.wafflestudio.team8server.preenroll.util.CourseScheduleUtil
import com.wafflestudio.team8server.user.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PracticeService(
    private val practiceLogRepository: PracticeLogRepository,
    private val practiceDetailRepository: PracticeDetailRepository,
    private val userRepository: UserRepository,
    private val courseRepository: CourseRepository,
    private val preEnrollRepository: PreEnrollRepository,
    private val practiceSessionService: PracticeSessionService,
    private val sessionConfig: PracticeSessionConfig,
    private val reactionTimePercentileService: ReactionTimePercentileService,
    private val enrollmentPeriodProperties: EnrollmentPeriodProperties,
    private val timeProvider: TimeProvider,
    private val leaderboardService: LeaderboardService,
) {
    private val objectMapper: ObjectMapper = ObjectMapper()

    /**
     * 수강신청 연습 세션을 시작합니다.
     * - PracticeLog를 생성하고 Redis에 세션을 저장합니다.
     * - 이미 활성 세션이 있으면 예외를 발생시킵니다.
     * - 동시성 제어를 위해 분산 락을 사용합니다.
     */
    @Transactional
    fun startPractice(
        userId: Long,
        startTimeOption: VirtualStartTimeOption,
    ): PracticeStartResponse {
        // 1. 분산 락 획득
        val lockAcquired = practiceSessionService.acquireLock(userId)
        if (!lockAcquired) {
            throw ActiveSessionExistsException("이미 세션 시작 처리 중입니다")
        }

        try {
            // 2. 이미 활성 세션이 있으면 리더보드 갱신 후 종료
            val existingPracticeLogId = practiceSessionService.getActiveSession(userId)
            if (existingPracticeLogId != null) {
                // 기존 세션의 리더보드 갱신
                leaderboardService.updateByPracticeEnd(userId, existingPracticeLogId)
                leaderboardService.updateWeeklyByPracticeEnd(userId, existingPracticeLogId)
                // 기존 세션 종료
                practiceSessionService.endSession(userId)
            }

            // 3. 사용자 조회
            val user =
                userRepository
                    .findById(userId)
                    .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다") }

            // 4. PracticeLog 생성
            val practiceLog = PracticeLog(user = user)
            val savedLog = practiceLogRepository.save(practiceLog)

            // 5. 세션 시작 시간 가져오기
            val startTimeMs = timeProvider.currentTimeMillis()

            // 6. 선택된 시작 시간 옵션에서 offset 가져오기
            val offsetMs = startTimeOption.offsetToTargetMs

            // 7. Redis에 세션 저장 (5분 TTL)
            practiceSessionService.createSession(userId, savedLog.id!!)
            practiceSessionService.saveStartTime(userId, startTimeMs)
            practiceSessionService.saveStartToTargetOffsetMs(userId, offsetMs)

            return PracticeStartResponse(
                practiceLogId = savedLog.id!!,
                virtualStartTime = startTimeOption.displayTime,
                targetTime = sessionConfig.targetTime,
                timeLimitSeconds = sessionConfig.timeLimitSeconds,
                message = "연습 세션이 시작되었습니다. 가상 시계가 ${startTimeOption.displayTime} 로 세팅되었습니다.",
            )
        } finally {
            // 8. 락 해제
            practiceSessionService.releaseLock(userId)
        }
    }

    /**
     * 수강신청 연습 세션을 종료합니다.
     * - Redis에서 세션을 삭제합니다.
     */
    @Transactional
    fun endPractice(userId: Long): PracticeEndResponse {
        // 1. 활성 세션 확인
        val practiceLogId =
            practiceSessionService.getActiveSession(userId)
                ?: throw NoActiveSessionException()

        // 2. 해당 세션의 시도 횟수 조회
        val totalAttempts = practiceDetailRepository.countByPracticeLogId(practiceLogId)

        // 2.1. 세션을 삭제하기 전에 리더보드 갱신
        leaderboardService.updateByPracticeEnd(
            userId = userId,
            practiceLogId = practiceLogId,
        )
        leaderboardService.updateWeeklyByPracticeEnd(
            userId = userId,
            practiceLogId = practiceLogId,
        )

        // 3. Redis에서 세션 삭제
        practiceSessionService.endSession(userId)

        return PracticeEndResponse(
            message = "연습 세션이 종료되었습니다.",
            totalAttempts = totalAttempts.toInt(),
        )
    }

    @Transactional
    fun attemptPractice(
        userId: Long,
        request: PracticeAttemptRequest,
    ): PracticeAttemptResponse {
        // 1. 활성 세션 확인
        val practiceLogId =
            practiceSessionService.getActiveSession(userId)
                ?: throw NoActiveSessionException("수강신청 기간이 아닙니다(연습 세션이 존재하지 않습니다)")

        // 2. 세션 시작 시간 조회
        val startTimeMs =
            practiceSessionService.getStartTime(userId)
                ?: throw ResourceNotFoundException("세션 시작 시간을 찾을 수 없습니다")

        // 3. 세션별 offset 조회
        val startToTargetOffsetMs =
            practiceSessionService.getStartToTargetOffsetMs(userId)
                ?: throw ResourceNotFoundException("세션 offset 정보를 찾을 수 없습니다")

        // 4. 서버 시간 기반 사용자 지연 시간 계산
        val currentTimeMs = timeProvider.currentTimeMillis()
        val userLatencyMs = (currentTimeMs - startTimeMs - startToTargetOffsetMs).toInt()

        // 5. PracticeLog 조회 (기존 세션 사용)
        val practiceLog =
            practiceLogRepository.findByIdOrNull(practiceLogId)
                ?: throw ResourceNotFoundException("연습 세션을 찾을 수 없습니다")

        // 6. Early Click 처리 (targetTime 기준 0ms 이하)
        if (userLatencyMs <= 0) {
            return handleEarlyClick(
                userLatencyMs = userLatencyMs,
                practiceLog = practiceLog,
            )
        }

        // 7. Course 존재 여부 검증
        val course =
            courseRepository.findByIdOrNull(request.courseId)
                ?: throw ResourceNotFoundException("강의를 찾을 수 없습니다 (ID: ${request.courseId})")

        // 7-1. PreEnroll에서 totalCompetitors(cartCount) 조회
        val preEnroll =
            preEnrollRepository.findByUserIdAndCourseId(userId, request.courseId)
                ?: throw ResourceNotFoundException("장바구니에 해당 강의가 없습니다")
        val totalCompetitors = preEnroll.cartCount

        // 7-2. 수강신청 기간 타입에 따른 유효 정원(capacity) 계산
        val capacity = course.getEffectiveQuota(enrollmentPeriodProperties.type)

        // 8. 중복 시도 체크 (log_id, course_id)
        val existingDetail = practiceDetailRepository.findByPracticeLogIdAndCourseId(practiceLogId, request.courseId)
        if (existingDetail != null) {
            // 이미 시도한 과목
            val message =
                if (existingDetail.isSuccess) {
                    "이미 수강신청된 강의입니다"
                } else {
                    "정원이 초과되었습니다(이미 시도한 강의입니다)"
                }
            return PracticeAttemptResponse(
                isSuccess = existingDetail.isSuccess,
                message = message,
                userLatencyMs = userLatencyMs,
            )
        }

        // 9. 시간 중복 검증 (이미 성공한 강의들과 비교)
        val enrolledCourses =
            practiceDetailRepository
                .findByPracticeLogIdOrderByReactionTimeAsc(practiceLogId)
                .filter { it.isSuccess && it.course != null }
                .mapNotNull { it.course }

        for (enrolledCourse in enrolledCourses) {
            val hasConflict =
                CourseScheduleUtil.hasTimeConflict(
                    objectMapper = objectMapper,
                    placeAndTimeJsonA = enrolledCourse.placeAndTime,
                    placeAndTimeJsonB = course.placeAndTime,
                )
            if (hasConflict) {
                return PracticeAttemptResponse(
                    isSuccess = false,
                    message = "시간이 겹치는 강의는 수강신청할 수 없습니다",
                    userLatencyMs = userLatencyMs,
                )
            }
        }

        // 10. 백분위(Percentile) 계산 (DB 기반)
        val percentile = reactionTimePercentileService.calculatePercentile(userLatencyMs)

        // 11. 등수(Rank) 산출
        val rank = reactionTimePercentileService.calculateRank(percentile, totalCompetitors)

        // 12. 성공 여부 판정
        val isSuccess = reactionTimePercentileService.isSuccessful(rank, capacity)

        // 13. PracticeDetail 저장 (통계 정보 포함, Course 정보 복사)
        val practiceDetail =
            PracticeDetail(
                practiceLog = practiceLog,
                course = course,
                courseTitle = course.courseTitle,
                lectureNumber = course.lectureNumber,
                isSuccess = isSuccess,
                reactionTime = userLatencyMs,
                rank = rank,
                percentile = percentile,
                capacity = capacity,
                totalCompetitors = totalCompetitors,
            )
        practiceDetailRepository.save(practiceDetail)

        // 16. 응답 반환 (간소화)
        val message =
            if (isSuccess) {
                "수강신청에 성공했습니다"
            } else {
                "정원이 초과되었습니다"
            }

        return PracticeAttemptResponse(
            isSuccess = isSuccess,
            message = message,
            userLatencyMs = userLatencyMs,
        )
    }

    /**
     * Early Click (너무 일찍 클릭한 경우)을 처리합니다.
     * earlyClickRecordingWindowMs 범위 내인 경우 PracticeLog에 기록하고,
     * 그 외에는 기록하지 않습니다.
     */
    private fun handleEarlyClick(
        userLatencyMs: Int,
        practiceLog: PracticeLog,
    ): PracticeAttemptResponse {
        // 일찍 클릭한 시간 (양수로 변환)
        val earlyClickAmount = kotlin.math.abs(userLatencyMs)

        // 기록 범위 내인지 확인 (예: 500ms <= 1000ms → 기록, 2000ms <= 1000ms → 기록 안 함)
        val shouldRecord = earlyClickAmount <= sessionConfig.earlyClickRecordingWindowMs

        if (shouldRecord) {
            // earlyClickRecordingWindowMs 범위 내: PracticeLog에 기록
            practiceLog.earlyClickDiff = userLatencyMs
            practiceLogRepository.save(practiceLog)
        }

        // earlyClickRecordingWindowMs 범위와 상관없이 동일한 메시지
        return PracticeAttemptResponse(
            isSuccess = false,
            message = "수강신청 기간이 아닙니다",
            userLatencyMs = userLatencyMs,
        )
    }

    /**
     * 연습 세션 결과를 조회합니다.
     * - practiceLogId로 연습 기록을 조회하고 모든 시도 내역을 반환합니다.
     * - 사용자는 자신의 연습 기록만 조회할 수 있습니다.
     */
    @Transactional(readOnly = true)
    fun getPracticeResults(
        userId: Long,
        practiceLogId: Long,
    ): PracticeResultResponse {
        // 1. PracticeLog 조회
        val practiceLog =
            practiceLogRepository.findByIdOrNull(practiceLogId)
                ?: throw ResourceNotFoundException("연습 기록을 찾을 수 없습니다")

        // 2. 본인의 기록인지 확인
        if (practiceLog.user.id != userId) {
            throw UnauthorizedException("다른 사용자의 연습 기록에 접근할 수 없습니다")
        }

        // 3. 모든 시도 내역 조회
        val details = practiceDetailRepository.findByPracticeLogIdOrderByReactionTimeAsc(practiceLogId)

        // 4. 성공 횟수 계산
        val successCount = details.count { it.isSuccess }

        // 5. DTO 변환 (PracticeDetail에 저장된 Course 정보 사용)
        val attempts =
            details.map { detail ->
                PracticeAttemptResult(
                    courseId = detail.course?.id,
                    courseTitle = detail.courseTitle,
                    lectureNumber = detail.lectureNumber,
                    isSuccess = detail.isSuccess,
                    rank = detail.rank,
                    percentile = detail.percentile,
                    reactionTime = detail.reactionTime,
                )
            }

        return PracticeResultResponse(
            practiceLogId = practiceLog.id!!,
            practiceAt = practiceLog.practiceAt.toString(),
            earlyClickDiff = practiceLog.earlyClickDiff,
            totalAttempts = details.size,
            successCount = successCount,
            attempts = attempts,
        )
    }

    /**
     * 가장 최근 연습 세션에서 성공한 강의 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    fun getEnrolledCourses(userId: Long): List<CourseDetailResponse> {
        // 1. attempt가 존재하는 가장 최근 PracticeLog 조회
        val practiceLog =
            practiceLogRepository.findFirstByUserIdWithAttemptsOrderByPracticeAtDesc(userId)
                ?: throw ResourceNotFoundException("연습 기록이 없습니다")

        val practiceLogId = practiceLog.id.ensureNotNull()

        // 2. 해당 세션의 성공한 시도들 조회
        val successfulDetails =
            practiceDetailRepository
                .findByPracticeLogIdOrderByReactionTimeAsc(practiceLogId)
                .filter { it.isSuccess && it.course != null }

        // 3. Course -> CourseDetailResponse 변환
        return successfulDetails
            .mapNotNull { it.course }
            .sortedWith(compareBy({ it.courseNumber }, { it.lectureNumber }))
            .map { course ->
                CourseDetailResponse(
                    id = course.id.ensureNotNull("강의 ID가 없습니다"),
                    year = course.year,
                    semester = course.semester,
                    classification = course.classification,
                    college = course.college,
                    department = course.department,
                    academicCourse = course.academicCourse,
                    academicYear = course.academicYear,
                    courseNumber = course.courseNumber,
                    lectureNumber = course.lectureNumber,
                    courseTitle = course.courseTitle,
                    credit = course.credit,
                    instructor = course.instructor,
                    placeAndTime = course.placeAndTime,
                    quota = course.quota,
                    freshmanQuota = course.freshmanQuota,
                )
            }
    }
}
