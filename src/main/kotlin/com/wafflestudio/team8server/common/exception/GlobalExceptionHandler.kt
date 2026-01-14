package com.wafflestudio.team8server.common.exception

import com.wafflestudio.team8server.course.service.CourseExcelParser
import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

@Schema(description = "에러 응답")
data class ErrorResponse(
    @Schema(description = "에러 발생 시각")
    val timestamp: LocalDateTime = LocalDateTime.now(),
    @Schema(description = "HTTP 상태 코드")
    val status: Int,
    @Schema(description = "HTTP 상태 이름")
    val error: String,
    @Schema(description = "사용자에게 표시할 에러 메시지")
    val message: String,
    @Schema(description = "에러 종류 식별자", nullable = true)
    val errorCode: String? = null,
    @Schema(description = "필드별 검증 에러 (유효성 검증 실패 시에만 포함)", nullable = true)
    val validationErrors: Map<String, String?>? = null,
)

@RestControllerAdvice
class GlobalExceptionHandler {
    // NullSafety에 위배된 예외 처리 -> 404 NOT FOUND (데이터 없음)
    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFoundException(e: ResourceNotFoundException): ResponseEntity<ErrorResponse> {
        val response =
            ErrorResponse(
                status = HttpStatus.NOT_FOUND.value(), // 404
                error = "Not Found",
                message = e.message,
                errorCode = e.errorCode,
            )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response)
    }

    // 이메일 중복 예외 처리 → 409 CONFLICT
    @ExceptionHandler(DuplicateEmailException::class)
    fun handleDuplicateEmailException(e: DuplicateEmailException): ResponseEntity<ErrorResponse> {
        val response =
            ErrorResponse(
                status = HttpStatus.CONFLICT.value(), // 409
                error = "Conflict",
                message = e.message, // "이미 사용 중.."
                errorCode = e.errorCode, // "DUPLICATE_EMAIL"
            )
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response)
    }

    // 유효성 검증 실패 예외 처리 → 400 BAD_REQUEST
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        // 모든 검증 에러를 Map으로 변환 (필드명 → 에러 메시지)
        val errors =
            e.bindingResult.allErrors.associate { error ->
                val field = (error as FieldError).field // 필드명 (email, password 등)
                val message = error.defaultMessage // 에러 메시지 (@NotBlank의 message)
                field to message // map pair 생성
            }

        val response =
            ErrorResponse(
                status = HttpStatus.BAD_REQUEST.value(), // 400
                error = "Bad Request",
                message = "입력 값이 유효하지 않습니다",
                errorCode = "VALIDATION_FAILED",
                validationErrors = errors, // {"email": "이메일은 필수입니다", ...}
            )
        return ResponseEntity.badRequest().body(response)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        val response =
            ErrorResponse(
                status = HttpStatus.BAD_REQUEST.value(),
                error = "Bad Request",
                message = "입력 값이 유효하지 않습니다",
                errorCode = "VALIDATION_FAILED",
                validationErrors = null,
            )
        return ResponseEntity.badRequest().body(response)
    }

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorizedException(e: UnauthorizedException): ResponseEntity<ErrorResponse> {
        val response =
            ErrorResponse(
                status = HttpStatus.UNAUTHORIZED.value(), // 401
                error = "Unauthorized",
                message = e.message ?: "인증에 실패했습니다",
                errorCode = "UNAUTHORIZED",
            )
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response)
    }

    @ExceptionHandler(NoActiveSessionException::class)
    fun handleNoActiveSessionException(e: NoActiveSessionException): ResponseEntity<ErrorResponse> {
        val response =
            ErrorResponse(
                status = HttpStatus.BAD_REQUEST.value(), // 400
                error = "Bad Request",
                message = e.message,
                errorCode = e.errorCode,
            )
        return ResponseEntity.badRequest().body(response)
    }

    @ExceptionHandler(ActiveSessionExistsException::class)
    fun handleActiveSessionExistsException(e: ActiveSessionExistsException): ResponseEntity<ErrorResponse> {
        val response =
            ErrorResponse(
                status = HttpStatus.CONFLICT.value(), // 409
                error = "Conflict",
                message = e.message,
                errorCode = e.errorCode,
            )
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response)
    }

    // 예상하지 못한 예외를 잡는 handler
    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(e: Exception): ResponseEntity<ErrorResponse> {
        // 배포 시 로깅 (logger.error("Unexpected error", e))
        val log = LoggerFactory.getLogger(CourseExcelParser::class.java)
        log.error("UNEXPECTED_ERROR", e)
        val response =
            ErrorResponse(
                status = HttpStatus.INTERNAL_SERVER_ERROR.value(), // 500
                error = "Internal Server Error",
                message = "예상하지 못한 오류가 발생했습니다",
                errorCode = "UNEXPECTED_ERROR",
            )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
    }

    @ExceptionHandler(
        PreEnrollAlreadyExistsException::class,
        DuplicateCourseNumberInPreEnrollException::class,
        TimeConflictInPreEnrollException::class,
    )
    fun handlePreEnrollConflict(e: BusinessException): ResponseEntity<ErrorResponse> {
        val response =
            ErrorResponse(
                status = HttpStatus.CONFLICT.value(),
                error = "Conflict",
                message = e.message,
                errorCode = e.errorCode,
            )
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response)
    }
}
