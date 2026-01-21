package com.wafflestudio.team8server.user.controller

import com.wafflestudio.team8server.common.auth.LoggedInUserId
import com.wafflestudio.team8server.common.exception.ErrorResponse
import com.wafflestudio.team8server.user.dto.PresignedUrlRequest
import com.wafflestudio.team8server.user.dto.PresignedUrlResponse
import com.wafflestudio.team8server.user.dto.UpdateProfileImageRequest
import com.wafflestudio.team8server.user.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "사용자 API", description = "사용자 프로필 관리 기능을 제공합니다")
@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
) {
    @Operation(
        summary = "프로필 이미지 업로드용 Presigned URL 생성",
        description =
            "S3에 프로필 이미지를 업로드하기 위한 Presigned URL을 생성합니다. " +
                "반환된 URL로 PUT 요청을 보내 이미지를 업로드할 수 있습니다.",
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Presigned URL 생성 성공",
                content = [Content(schema = Schema(implementation = PresignedUrlResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "유효성 검증 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "validation-error",
                                summary = "유효성 검증 실패",
                                value = """
                                {
                                  "timestamp": "2026-01-21T12:00:00",
                                  "status": 400,
                                  "error": "Bad Request",
                                  "message": "입력 값이 유효하지 않습니다",
                                  "errorCode": "VALIDATION_FAILED",
                                  "validationErrors": {
                                    "extension": "지원하지 않는 파일 형식입니다"
                                  }
                                }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "unauthorized",
                                summary = "인증 실패",
                                value = """
                                {
                                  "timestamp": "2026-01-21T12:00:00",
                                  "status": 401,
                                  "error": "UNAUTHORIZED",
                                  "message": "인증에 실패했습니다",
                                  "errorCode": "UNAUTHORIZED",
                                  "validationErrors": null
                                }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    @PostMapping("/me/profile-image/presigned-url")
    @ResponseStatus(HttpStatus.OK)
    fun generatePresignedUrl(
        @Parameter(hidden = true) @LoggedInUserId userId: Long,
        @Valid @RequestBody request: PresignedUrlRequest,
    ): PresignedUrlResponse = userService.generatePresignedUrl(userId, request)

    @Operation(
        summary = "프로필 이미지 URL 저장",
        description =
            "S3에 업로드 완료된 이미지의 URL을 DB에 저장합니다. " +
                "기존 프로필 이미지가 있으면 S3에서 삭제됩니다.",
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "프로필 이미지 저장 성공",
            ),
            ApiResponse(
                responseCode = "400",
                description = "유효성 검증 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "validation-error",
                                summary = "유효성 검증 실패",
                                value = """
                                {
                                  "timestamp": "2026-01-21T12:00:00",
                                  "status": 400,
                                  "error": "Bad Request",
                                  "message": "입력 값이 유효하지 않습니다",
                                  "errorCode": "VALIDATION_FAILED",
                                  "validationErrors": {
                                    "imageUrl": "이미지 URL은 필수입니다"
                                  }
                                }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "사용자를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "user-not-found",
                                summary = "사용자 없음",
                                value = """
                                {
                                  "timestamp": "2026-01-21T12:00:00",
                                  "status": 404,
                                  "error": "Not Found",
                                  "message": "사용자를 찾을 수 없습니다",
                                  "errorCode": "USER_NOT_FOUND",
                                  "validationErrors": null
                                }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    @PatchMapping("/me/profile-image")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateProfileImage(
        @Parameter(hidden = true) @LoggedInUserId userId: Long,
        @Valid @RequestBody request: UpdateProfileImageRequest,
    ) = userService.updateProfileImage(userId, request)

    @Operation(
        summary = "프로필 이미지 삭제",
        description = "프로필 이미지를 삭제합니다. S3에서 이미지가 삭제되고 DB에서 URL이 제거됩니다.",
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "프로필 이미지 삭제 성공",
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "사용자를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
    @DeleteMapping("/me/profile-image")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteProfileImage(
        @Parameter(hidden = true) @LoggedInUserId userId: Long,
    ) = userService.deleteProfileImage(userId)
}
