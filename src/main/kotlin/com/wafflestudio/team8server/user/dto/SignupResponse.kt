package com.wafflestudio.team8server.user.dto

import com.wafflestudio.team8server.user.dto.coreDto.UserDto
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "회원가입 응답")
data class SignupResponse(
    @Schema(description = "사용자 정보")
    val user: UserDto,
    @Schema(
        description = "JWT 액세스 토큰",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwiZW1haWwiOiJ1c2VyQGV4YW1wbGUuY29tIn0.abc123",
    )
    val accessToken: String,
)
