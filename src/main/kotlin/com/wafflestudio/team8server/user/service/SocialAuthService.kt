package com.wafflestudio.team8server.user.service

import com.wafflestudio.team8server.common.exception.UnauthorizedException
import com.wafflestudio.team8server.common.extension.ensureNotNull
import com.wafflestudio.team8server.user.JwtTokenProvider
import com.wafflestudio.team8server.user.dto.LoginResponse
import com.wafflestudio.team8server.user.dto.coreDto.UserDto
import com.wafflestudio.team8server.user.enum.SocialProvider
import com.wafflestudio.team8server.user.model.SocialCredential
import com.wafflestudio.team8server.user.model.User
import com.wafflestudio.team8server.user.repository.SocialCredentialRepository
import com.wafflestudio.team8server.user.repository.UserRepository
import com.wafflestudio.team8server.user.service.social.google.GoogleIdTokenVerifier
import com.wafflestudio.team8server.user.service.social.google.GoogleOAuthClient
import com.wafflestudio.team8server.user.service.social.kakao.KakaoOAuthClient
import com.wafflestudio.team8server.user.util.NicknameGenerator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SocialAuthService(
    private val userRepository: UserRepository,
    private val socialCredentialRepository: SocialCredentialRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val kakaoOAuthClient: KakaoOAuthClient,
    private val googleOAuthClient: GoogleOAuthClient,
    private val googleIdTokenVerifier: GoogleIdTokenVerifier,
) {
    @Transactional
    fun kakaoLogin(
        code: String,
        redirectUri: String?,
    ): LoginResponse {
        val kakaoUserInfo =
            try {
                kakaoOAuthClient.getUserInfo(code, redirectUri)
            } catch (e: Exception) {
                throw UnauthorizedException("카카오 인증에 실패했습니다")
            }

        // 기존 SocialCredential 조회
        val provider = SocialProvider.KAKAO.dbValue()
        val socialId = kakaoUserInfo.id

        val existingCredential = socialCredentialRepository.findByProviderAndSocialId(provider, socialId)
        if (existingCredential != null) {
            val accessToken = jwtTokenProvider.createToken(existingCredential.user.id.ensureNotNull(), existingCredential.user.role.name)
            return LoginResponse(
                accessToken = accessToken,
                user = UserDto.from(existingCredential.user),
            )
        }

        // 신규 회원인 경우 User 생성 및 저장
        val user =
            User(
                nickname = kakaoUserInfo.nickname ?: NicknameGenerator.generateRandomNickname(),
                profileImageUrl = null, // kakaoUserInfo.profileImageUrl,
            )
        val savedUser = userRepository.save(user)

        // SocialCredential 생성 및 저장
        val credential =
            SocialCredential(
                user = savedUser,
                provider = provider,
                socialId = socialId,
            )
        socialCredentialRepository.save(credential)

        // JWT 발급 및 응답
        val accessToken = jwtTokenProvider.createToken(savedUser.id.ensureNotNull(), savedUser.role.name)
        return LoginResponse(
            accessToken = accessToken,
            user = UserDto.from(savedUser),
        )
    }

    @Transactional
    fun googleLogin(
        code: String,
        redirectUri: String?,
    ): LoginResponse {
        val tokenResult =
            try {
                googleOAuthClient.exchangeCodeForTokenResult(code, redirectUri)
            } catch (e: Exception) {
                throw UnauthorizedException("구글 인증에 실패했습니다")
            }

        val idToken = tokenResult.idToken

        val googleUserInfo =
            try {
                googleIdTokenVerifier.verifyAndExtract(idToken)
            } catch (e: Exception) {
                throw UnauthorizedException("구글 인증에 실패했습니다")
            }

        val provider = SocialProvider.GOOGLE.dbValue()
        val socialId = googleUserInfo.sub

        val existingCredential = socialCredentialRepository.findByProviderAndSocialId(provider, socialId)
        if (existingCredential != null) {
            // refresh_token은 응답에 포함되지 않는 경우가 흔하므로, null이면 기존 값을 보존한다.
            if (!tokenResult.refreshToken.isNullOrBlank()) {
                existingCredential.refreshToken = tokenResult.refreshToken
            }
            val accessToken = jwtTokenProvider.createToken(existingCredential.user.id.ensureNotNull(), existingCredential.user.role.name)
            return LoginResponse(
                accessToken = accessToken,
                user = UserDto.from(existingCredential.user),
            )
        }

        val user =
            User(
                nickname = googleUserInfo.name ?: NicknameGenerator.generateRandomNickname(),
                profileImageUrl = null,
            )
        val savedUser = userRepository.save(user)

        val credential =
            SocialCredential(
                user = savedUser,
                provider = provider,
                socialId = socialId,
                refreshToken = tokenResult.refreshToken,
            )
        socialCredentialRepository.save(credential)

        val accessToken = jwtTokenProvider.createToken(savedUser.id.ensureNotNull(), savedUser.role.name)
        return LoginResponse(
            accessToken = accessToken,
            user = UserDto.from(savedUser),
        )
    }
}
