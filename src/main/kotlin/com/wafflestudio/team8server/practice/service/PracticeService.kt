package com.wafflestudio.team8server.practice.service

import com.wafflestudio.team8server.common.exception.ActiveSessionExistsException
import com.wafflestudio.team8server.common.exception.NoActiveSessionException
import com.wafflestudio.team8server.common.exception.PracticeTimeExpiredException
import com.wafflestudio.team8server.common.exception.ResourceNotFoundException
import com.wafflestudio.team8server.practice.dto.PracticeAttemptRequest
import com.wafflestudio.team8server.practice.dto.PracticeAttemptResponse
import com.wafflestudio.team8server.practice.dto.PracticeEndResponse
import com.wafflestudio.team8server.practice.dto.PracticeStartResponse
import com.wafflestudio.team8server.practice.model.PracticeDetail
import com.wafflestudio.team8server.practice.model.PracticeLog
import com.wafflestudio.team8server.practice.repository.PracticeDetailRepository
import com.wafflestudio.team8server.practice.repository.PracticeLogRepository
import com.wafflestudio.team8server.practice.util.LogNormalDistributionUtil
import com.wafflestudio.team8server.user.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PracticeService(
    private val practiceLogRepository: PracticeLogRepository,
    private val practiceDetailRepository: PracticeDetailRepository,
    private val userRepository: UserRepository,
    private val practiceSessionService: PracticeSessionService,
) {
    companion object {
        private const val VIRTUAL_START_TIME = "08:28:00"
        private const val TARGET_TIME = "08:30:00"
        private const val PRACTICE_TIME_LIMIT_MS = 300000L // 5분 (08:28:00 ~ 08:33:00)
        private const val VIRTUAL_TARGET_TIME_OFFSET_MS = 120000L // 2분 (08:28:00 ~ 08:30:00)
        private const val EARLY_CLICK_THRESHOLD_MS = -1000 // -1초
        private const val TARGET_TIME_MS = 0 // 08:30:00 기준
    }

    /**
     * 수강신청 연습 세션을 시작합니다.
     * - PracticeLog를 생성하고 Redis에 세션을 저장합니다.
     * - 이미 활성 세션이 있으면 예외를 발생시킵니다.
     * - 동시성 제어를 위해 분산 락을 사용합니다.
     */
    @Transactional
    fun startPractice(): PracticeStartResponse {
        val userId = getCurrentUserId()

        // 1. 분산 락 획득
        val lockAcquired = practiceSessionService.acquireLock(userId)
        if (!lockAcquired) {
            throw ActiveSessionExistsException("이미 세션 시작 처리 중입니다")
        }

        try {
            // 2. 이미 활성 세션이 있는지 확인
            if (practiceSessionService.hasActiveSession(userId)) {
                throw ActiveSessionExistsException()
            }

            // 3. 사용자 조회
            val user =
                userRepository
                    .findById(userId)
                    .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다") }

            // 4. PracticeLog 생성
            val practiceLog = PracticeLog(user = user)
            val savedLog = practiceLogRepository.save(practiceLog)

            // 5. Redis에 세션 저장 (5분 TTL)
            practiceSessionService.createSession(userId, savedLog.id!!)

            return PracticeStartResponse(
                practiceLogId = savedLog.id!!,
                virtualStartTime = VIRTUAL_START_TIME,
                targetTime = TARGET_TIME,
                timeLimit = "08:33:00",
                message = "연습 세션이 시작되었습니다. 가상 시계가 $VIRTUAL_START_TIME 로 세팅되었습니다.",
            )
        } finally {
            // 6. 락 해제
            practiceSessionService.releaseLock(userId)
        }
    }

    /**
     * 수강신청 연습 세션을 종료합니다.
     * - Redis에서 세션을 삭제합니다.
     */
    @Transactional(readOnly = true)
    fun endPractice(): PracticeEndResponse {
        val userId = getCurrentUserId()

        // 1. 활성 세션 확인
        val practiceLogId =
            practiceSessionService.getActiveSession(userId)
                ?: throw NoActiveSessionException()

        // 2. 해당 세션의 시도 횟수 조회
        val practiceLog =
            practiceLogRepository.findByIdOrNull(practiceLogId)
                ?: throw ResourceNotFoundException("연습 세션을 찾을 수 없습니다")

        val totalAttempts = practiceDetailRepository.countByPracticeLog(practiceLog)

        // 3. Redis에서 세션 삭제
        practiceSessionService.endSession(userId)

        return PracticeEndResponse(
            message = "연습 세션이 종료되었습니다.",
            totalAttempts = totalAttempts.toInt(),
        )
    }

    @Transactional
    fun attemptPractice(request: PracticeAttemptRequest): PracticeAttemptResponse {
        val userId = getCurrentUserId()

        // 1. 활성 세션 확인
        val practiceLogId =
            practiceSessionService.getActiveSession(userId)
                ?: throw NoActiveSessionException()

        // 2. Session & Time Validation (시간 유효성 검사)
        validatePracticeTime(userId)

        // 3. PracticeLog 조회 (기존 세션 사용)
        val practiceLog =
            practiceLogRepository.findByIdOrNull(practiceLogId)
                ?: throw ResourceNotFoundException("연습 세션을 찾을 수 없습니다")

        // TODO: Course API 구현 후 주석 해제
        // 4. Course 존재 여부 검증
        // val course = courseRepository.findByIdOrNull(request.courseId)
        //     ?: throw ResourceNotFoundException("강의를 찾을 수 없습니다")

        // 5. Early Click 처리
        if (request.userLatencyMs <= TARGET_TIME_MS) {
            return handleEarlyClick(
                request = request,
                practiceLog = practiceLog,
            )
        }

        // 6. 로그정규분포 계산
        val distributionUtil =
            LogNormalDistributionUtil(
                scale = request.scale,
                shape = request.shape,
            )

        // 7. 백분위(Percentile) 계산
        val percentile = distributionUtil.calculatePercentile(request.userLatencyMs)

        // 8. 등수(Rank) 산출
        val rank = distributionUtil.calculateRank(percentile, request.totalCompetitors)

        // 9. 성공 여부 판정
        val isSuccess = distributionUtil.isSuccessful(rank, request.capacity)

        // 10. PracticeDetail 저장
        val practiceDetail =
            PracticeDetail(
                practiceLog = practiceLog,
                courseId = request.courseId,
                isSuccess = isSuccess,
                reactionTime = request.userLatencyMs,
                earlyClickDiff = null,
            )
        practiceDetailRepository.save(practiceDetail)

        // 11. 응답 반환
        val message =
            if (isSuccess) {
                "수강신청에 성공했습니다! (${rank}등 / ${request.totalCompetitors}명)"
            } else {
                "수강신청에 실패했습니다. (${rank}등 / ${request.totalCompetitors}명, 정원: ${request.capacity}명)"
            }

        return PracticeAttemptResponse(
            isSuccess = isSuccess,
            message = message,
            rank = rank,
            percentile = percentile,
            reactionTime = request.userLatencyMs,
            earlyClickDiff = null,
        )
    }

    /**
     * 연습 시간이 유효한지 검증합니다.
     * 세션 시작 후 5분(300000ms)이 초과되면 예외를 발생시킵니다.
     */
    private fun validatePracticeTime(userId: Long) {
        val startTime =
            practiceSessionService.getSessionStartTime(userId)
                ?: throw NoActiveSessionException()

        val elapsedTimeMs = System.currentTimeMillis() - startTime

        if (elapsedTimeMs >= PRACTICE_TIME_LIMIT_MS) {
            throw PracticeTimeExpiredException()
        }
    }

    /**
     * Early Click (너무 일찍 클릭한 경우)을 처리합니다.
     * -1000ms ~ 0ms 사이인 경우 DB에 기록하고,
     * 그 외에는 기록하지 않습니다.
     */
    private fun handleEarlyClick(
        request: PracticeAttemptRequest,
        practiceLog: PracticeLog,
    ): PracticeAttemptResponse {
        val shouldRecord = request.userLatencyMs >= EARLY_CLICK_THRESHOLD_MS

        if (shouldRecord) {
            // -1000ms ~ 0ms 사이: DB에 기록
            val practiceDetail =
                PracticeDetail(
                    practiceLog = practiceLog,
                    courseId = request.courseId,
                    isSuccess = false,
                    reactionTime = 0, // Early click이므로 reactionTime은 0으로 설정
                    earlyClickDiff = request.userLatencyMs, // 음수 값으로 저장
                )
            practiceDetailRepository.save(practiceDetail)

            val message = "너무 일찍 클릭했습니다! (${kotlin.math.abs(request.userLatencyMs)}ms 일찍 클릭)"

            return PracticeAttemptResponse(
                isSuccess = false,
                message = message,
                rank = null,
                percentile = null,
                reactionTime = 0,
                earlyClickDiff = request.userLatencyMs,
            )
        } else {
            // < -1000ms: DB에 기록하지 않음
            val message = "너무 일찍 클릭했습니다!"

            return PracticeAttemptResponse(
                isSuccess = false,
                message = message,
                rank = null,
                percentile = null,
                reactionTime = 0,
                earlyClickDiff = null,
            )
        }
    }

    /**
     * SecurityContext에서 현재 로그인한 사용자의 ID를 가져옵니다.
     */
    private fun getCurrentUserId(): Long {
        val authentication =
            SecurityContextHolder.getContext().authentication
                ?: throw ResourceNotFoundException("인증 정보를 찾을 수 없습니다")
        return authentication.principal as Long
    }
}
