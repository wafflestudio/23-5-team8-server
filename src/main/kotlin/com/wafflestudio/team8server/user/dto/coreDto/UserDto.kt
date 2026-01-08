package com.wafflestudio.team8server.user.dto.coreDto

import com.wafflestudio.team8server.common.extension.ensureNotNull
import com.wafflestudio.team8server.user.model.User
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사용자 정보")
data class UserDto(
    @Schema(description = "사용자 ID", example = "1")
    val id: Long?,
    @Schema(description = "사용자 닉네임", example = "홍길동")
    val nickname: String,
) {
    companion object {
        fun from(user: User): UserDto =
            UserDto(
                id = user.id.ensureNotNull(),
                nickname = user.nickname,
            )
    }
}
