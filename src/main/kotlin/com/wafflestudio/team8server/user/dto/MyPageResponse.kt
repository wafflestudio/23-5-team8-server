package com.wafflestudio.team8server.user.dto

import com.wafflestudio.team8server.user.model.User
import com.wafflestudio.team8server.user.service.ProfileImageUrlResolver
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "마이페이지 조회 응답")
data class MyPageResponse(
    @Schema(description = "사용자 닉네임", example = "홍길동")
    val nickname: String,
    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
    val profileImageUrl: String?,
) {
    companion object {
        fun from(
            user: User,
            resolver: ProfileImageUrlResolver,
        ): MyPageResponse =
            MyPageResponse(
                nickname = user.nickname,
                profileImageUrl = resolver.resolve(user.profileImageUrl),
            )
    }
}
