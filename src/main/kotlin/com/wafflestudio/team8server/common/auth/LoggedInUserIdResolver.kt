package com.wafflestudio.team8server.common.auth

import com.wafflestudio.team8server.common.exception.UnauthorizedException
import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

/**
 * @LoggedInUserId 어노테이션이 붙은 파라미터에 현재 로그인한 사용자의 ID를 주입하는 Resolver입니다.
 *
 * SecurityContext에서 인증 정보를 가져와 userId(Long)를 반환합니다.
 */
@Component
class LoggedInUserIdResolver : HandlerMethodArgumentResolver {
    /**
     * @LoggedInUserId 어노테이션이 붙은 파라미터를 지원합니다.
     */
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(LoggedInUserId::class.java)

    /**
     * SecurityContext에서 현재 로그인한 사용자의 ID를 가져옵니다.
     *
     * @throws UnauthorizedException 인증 정보가 없거나 유효하지 않은 경우
     */
    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Long {
        val authentication =
            SecurityContextHolder.getContext().authentication
                ?: throw UnauthorizedException("인증 정보를 찾을 수 없습니다")

        // principal 타입 확인 및 안전한 캐스팅
        return when (val principal = authentication.principal) {
            is Long -> principal
            is String ->
                principal.toLongOrNull()
                    ?: throw UnauthorizedException("유효하지 않은 사용자 ID 형식: $principal")
            else -> throw UnauthorizedException("지원하지 않는 principal 타입: ${principal?.let { it::class.simpleName } ?: "null"}")
        }
    }
}
