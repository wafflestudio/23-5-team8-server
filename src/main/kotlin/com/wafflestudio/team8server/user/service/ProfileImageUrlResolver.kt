package com.wafflestudio.team8server.user.service

import com.wafflestudio.team8server.config.S3Properties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * 프로필 이미지 URL을 해석하는 컴포넌트.
 * - 이미 full URL인 경우 (소셜 로그인 프로필): 그대로 반환
 * - S3 key인 경우: base URL을 붙여서 full URL로 변환
 */
@Component
class ProfileImageUrlResolver(
    @Autowired(required = false)
    private val s3Properties: S3Properties?,
) {
    fun resolve(profileImageUrl: String?): String? {
        if (profileImageUrl == null) return null

        // 이미 full URL인 경우 (소셜 로그인 등 외부 URL)
        if (profileImageUrl.startsWith("http://") || profileImageUrl.startsWith("https://")) {
            return profileImageUrl
        }

        // S3 key인 경우 full URL로 변환
        val baseUrl = s3Properties?.baseUrl?.trimEnd('/') ?: return profileImageUrl
        return "$baseUrl/$profileImageUrl"
    }
}
