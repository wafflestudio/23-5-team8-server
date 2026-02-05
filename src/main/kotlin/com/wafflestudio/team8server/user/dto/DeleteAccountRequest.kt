package com.wafflestudio.team8server.user.dto

import io.swagger.v3.oas.annotations.media.Schema

data class DeleteAccountRequest(
    @Schema(
        description = "로컬(이메일) 가입 유저의 경우에만 필수",
        example = "myPassword123!",
        nullable = true,
    )
    val password: String? = null,
)
