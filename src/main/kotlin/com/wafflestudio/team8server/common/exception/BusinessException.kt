package com.wafflestudio.team8server.common.exception

// 모든 케이스를 컴파일러가 체크
sealed class BusinessException(
    override val message: String,
    val errorCode: String,
) : RuntimeException(message)

// NOTNULL이어야 하는 곳에서 NULL이 감지되는 예외
class ResourceNotFoundException(
    message: String,
) : BusinessException(
        message = message,
        errorCode = "RESOURCE_NOT_FOUND",
    )

// 이메일 중복 예외
class DuplicateEmailException(
    email: String,
) : BusinessException(
        message = "이미 사용 중인 이메일입니다: $email",
        errorCode = "DUPLICATE_EMAIL", // client가 식별할 에러 코드
    )

// 로그인 실패
class UnauthorizedException(
    message: String,
) : BusinessException(
        message = message,
        errorCode = "UNAUTHORIZED",
    )

// 소셜 로그인 시 이메일 누락
class SocialEmailRequiredException :
    BusinessException(
        message = "소셜 로그인에서 이메일 제공이 필요합니다",
        errorCode = "SOCIAL_EMAIL_REQUIRED",
    )
