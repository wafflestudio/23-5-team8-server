package com.wafflestudio.team8server.user.service

import com.wafflestudio.team8server.common.dto.PageInfo
import com.wafflestudio.team8server.common.exception.ResourceNotFoundException
import com.wafflestudio.team8server.common.exception.UnauthorizedException
import com.wafflestudio.team8server.common.extension.ensureNotNull
import com.wafflestudio.team8server.practice.dto.PracticeAttemptResult
import com.wafflestudio.team8server.practice.dto.PracticeResultResponse
import com.wafflestudio.team8server.practice.dto.PracticeSessionItem
import com.wafflestudio.team8server.practice.dto.PracticeSessionListResponse
import com.wafflestudio.team8server.practice.repository.PracticeDetailRepository
import com.wafflestudio.team8server.practice.repository.PracticeLogRepository
import com.wafflestudio.team8server.user.dto.ChangePasswordRequest
import com.wafflestudio.team8server.user.dto.MyPageResponse
import com.wafflestudio.team8server.user.dto.UpdateProfileRequest
import com.wafflestudio.team8server.user.repository.LocalCredentialRepository
import com.wafflestudio.team8server.user.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MyPageService(
    private val userRepository: UserRepository,
    private val localCredentialRepository: LocalCredentialRepository,
    private val passwordEncoder: PasswordEncoder,
    private val practiceLogRepository: PracticeLogRepository,
    private val practiceDetailRepository: PracticeDetailRepository,
) {
    @Transactional(readOnly = true)
    fun getMyPage(userId: Long): MyPageResponse {
        val user =
            userRepository
                .findById(userId)
                .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다") }
        return MyPageResponse.from(user)
    }

    @Transactional
    fun updateProfile(
        userId: Long,
        request: UpdateProfileRequest,
    ): MyPageResponse {
        if (request.nickname == null && request.profileImageUrl == null) {
            throw ResourceNotFoundException("수정할 항목이 없습니다")
        }

        val user =
            userRepository
                .findById(userId)
                .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다") }

        request.nickname?.let { user.nickname = it }
        request.profileImageUrl?.let { user.profileImageUrl = it }

        return MyPageResponse.from(user)
    }

    @Transactional
    fun changePassword(
        userId: Long,
        request: ChangePasswordRequest,
    ) {
        val credential =
            localCredentialRepository.findByUserId(userId)
                ?: throw ResourceNotFoundException(
                    "로컬 계정 정보를 찾을 수 없습니다. 소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다.",
                )

        if (!passwordEncoder.matches(request.currentPassword, credential.passwordHash)) {
            throw UnauthorizedException("현재 비밀번호가 일치하지 않습니다")
        }

        credential.passwordHash = passwordEncoder.encode(request.newPassword).ensureNotNull()
    }

    @Transactional(readOnly = true)
    fun getPracticeSessions(
        userId: Long,
        page: Int,
        size: Int,
    ): PracticeSessionListResponse {
        val pageable = PageRequest.of(page, size)
        val practiceLogs = practiceLogRepository.findByUserIdOrderByPracticeAtDesc(userId, pageable)

        val items =
            practiceLogs.content.map { log ->
                val totalAttempts = practiceDetailRepository.countByPracticeLogId(log.id!!).toInt()
                val successCount = practiceDetailRepository.countByPracticeLogIdAndIsSuccess(log.id!!, true).toInt()

                PracticeSessionItem(
                    id = log.id!!,
                    practiceAt = log.practiceAt,
                    totalAttempts = totalAttempts,
                    successCount = successCount,
                )
            }

        return PracticeSessionListResponse(
            items = items,
            pageInfo =
                PageInfo(
                    page = practiceLogs.number,
                    size = practiceLogs.size,
                    totalElements = practiceLogs.totalElements,
                    totalPages = practiceLogs.totalPages,
                    hasNext = practiceLogs.hasNext(),
                ),
        )
    }

    @Transactional(readOnly = true)
    fun getPracticeSessionDetail(
        userId: Long,
        practiceLogId: Long,
    ): PracticeResultResponse {
        val practiceLog =
            practiceLogRepository.findByIdOrNull(practiceLogId)
                ?: throw ResourceNotFoundException("연습 기록을 찾을 수 없습니다")

        if (practiceLog.user.id != userId) {
            throw UnauthorizedException("다른 사용자의 연습 기록에 접근할 수 없습니다")
        }

        val details = practiceDetailRepository.findByPracticeLogId(practiceLogId)
        val successCount = details.count { it.isSuccess }

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
            practiceLogId = practiceLog.id.ensureNotNull(),
            practiceAt = practiceLog.practiceAt.toString(),
            earlyClickDiff = practiceLog.earlyClickDiff,
            totalAttempts = details.size,
            successCount = successCount,
            attempts = attempts,
        )
    }
}
