package com.wafflestudio.team8server.user.service

import com.wafflestudio.team8server.common.exception.DuplicateEmailException
import com.wafflestudio.team8server.common.exception.UnauthorizedException
import com.wafflestudio.team8server.common.extension.ensureNotNull
import com.wafflestudio.team8server.practice.service.PracticeService
import com.wafflestudio.team8server.user.JwtTokenProvider
import com.wafflestudio.team8server.user.dto.LoginRequest
import com.wafflestudio.team8server.user.dto.LoginResponse
import com.wafflestudio.team8server.user.dto.SignupRequest
import com.wafflestudio.team8server.user.dto.SignupResponse
import com.wafflestudio.team8server.user.dto.coreDto.UserDto
import com.wafflestudio.team8server.user.model.LocalCredential
import com.wafflestudio.team8server.user.model.User
import com.wafflestudio.team8server.user.repository.LocalCredentialRepository
import com.wafflestudio.team8server.user.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val localCredentialRepository: LocalCredentialRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val tokenBlacklistService: TokenBlacklistService,
    private val practiceService: PracticeService,
) {
    @Transactional
    fun signup(request: SignupRequest): SignupResponse {
        // 1. 이메일 중복 체크
        if (localCredentialRepository.existsByEmail(request.email)) {
            throw DuplicateEmailException(request.email) // 409 CONFLICT
        }

        // 2. User 엔티티 생성 및 저장
        val user =
            User(
                nickname = request.nickname,
                profileImageUrl = request.profileImageUrl,
            )
        val savedUser = userRepository.save(user) // id가 0 → DB가 부여한 실제 ID로 변경됨

        // 3. 비밀번호 해싱
        val hashedPassword: String = passwordEncoder.encode(request.password)!!

        // 4. LocalCredential 엔티티 생성 및 저장
        val credential =
            LocalCredential(
                user = savedUser,
                email = request.email,
                passwordHash = hashedPassword,
            )
        localCredentialRepository.save(credential)

        val accessToken = jwtTokenProvider.createToken(savedUser.id.ensureNotNull())

        // 5. 응답 DTO 반환
        return SignupResponse(
            accessToken = accessToken,
            user = UserDto.from(user),
        )
    }

    fun login(request: LoginRequest): LoginResponse {
        // 1. 이메일로 LocalCredential 조회
        val credential =
            localCredentialRepository.findByEmail(request.email)
                ?: throw UnauthorizedException("이메일 또는 비밀번호가 일치하지 않습니다")

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(request.password, credential.passwordHash)) {
            throw UnauthorizedException("이메일 또는 비밀번호가 일치하지 않습니다")
        }

        // 3. JWT 토큰 발급
        val accessToken = jwtTokenProvider.createToken(credential.user.id.ensureNotNull())

        // 4. 응답 반환
        return LoginResponse(
            accessToken = accessToken,
            user = UserDto.from(credential.user),
        )
    }

    fun logout(token: String) {
        // 1. 토큰 유효성 검증
        if (!jwtTokenProvider.validateToken(token)) {
            throw UnauthorizedException("유효하지 않은 토큰입니다")
        }

        // 2. 연습 세션이 있으면 종료
        val userId = jwtTokenProvider.getUserIdFromToken(token)
        practiceService.endPracticeInternal(userId)

        // 3. 토큰의 남은 유효 시간 계산
        val remainingTime = jwtTokenProvider.getRemainingExpirationTime(token)

        // 4. 토큰을 블랙리스트에 추가
        tokenBlacklistService.addToBlacklist(token, remainingTime)
    }
}
