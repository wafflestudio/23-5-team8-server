package com.wafflestudio.team8server.config

import com.wafflestudio.team8server.user.JwtTokenProvider
import com.wafflestudio.team8server.user.service.TokenBlacklistService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val tokenBlacklistService: TokenBlacklistService,
) : OncePerRequestFilter() {
    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            // 1. 요청 헤더에서 JWT 토큰 추출
            val token = extractTokenFromRequest(request)

            // 2. 토큰이 존재하고 유효한 경우
            if (token != null && jwtTokenProvider.validateToken(token)) {
                // 3. 토큰이 블랙리스트에 있는지 확인 (로그아웃된 토큰인지 체크)
                if (!tokenBlacklistService.isBlacklisted(token)) {
                    // 4. 토큰에서 사용자 ID와 권한 추출
                    val userId = jwtTokenProvider.getUserIdFromToken(token)
                    val role = jwtTokenProvider.getRoleFromToken(token)
                    val authorities = listOf(SimpleGrantedAuthority("ROLE_$role"))

                    // 5. 인증 객체 생성
                    val authentication =
                        UsernamePasswordAuthenticationToken(
                            userId, // principal: 사용자 ID
                            null, // credentials: 비밀번호 (JWT에서는 불필요)
                            authorities, // authorities: 권한 목록
                        )

                    // 6. 요청 정보를 인증 객체에 추가
                    authentication.details = WebAuthenticationDetailsSource().buildDetails(request)

                    // 7. SecurityContext에 인증 정보 저장
                    SecurityContextHolder.getContext().authentication = authentication
                }
            }
        } catch (e: Exception) {
            // 토큰 검증 실패 시 로그만 남기고 인증 실패 처리
            logger.error("JWT 토큰 검증 실패: ${e.message}")
        }

        filterChain.doFilter(request, response)
    }

    private fun extractTokenFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader(AUTHORIZATION_HEADER)

        return if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            bearerToken.substring(BEARER_PREFIX.length)
        } else {
            null
        }
    }
}
