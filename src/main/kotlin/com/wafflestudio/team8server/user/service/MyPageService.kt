package com.wafflestudio.team8server.user.service

import com.wafflestudio.team8server.common.exception.ResourceNotFoundException
import com.wafflestudio.team8server.common.exception.UnauthorizedException
import com.wafflestudio.team8server.common.extension.ensureNotNull
import com.wafflestudio.team8server.user.dto.ChangePasswordRequest
import com.wafflestudio.team8server.user.dto.MyPageResponse
import com.wafflestudio.team8server.user.dto.UpdateProfileRequest
import com.wafflestudio.team8server.user.repository.LocalCredentialRepository
import com.wafflestudio.team8server.user.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MyPageService(
    private val userRepository: UserRepository,
    private val localCredentialRepository: LocalCredentialRepository,
    private val passwordEncoder: PasswordEncoder,
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
}
