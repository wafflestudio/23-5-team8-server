package com.wafflestudio.team8server.user.service

import com.wafflestudio.team8server.config.OciStorageProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * 프로필 이미지 URL을 해석하는 컴포넌트.
 * - 이미 full URL인 경우 (소셜 로그인 프로필): 그대로 반환
 * - Object Storage key인 경우: base URL을 붙여서 full URL로 변환
 */
@Component
class ProfileImageUrlResolver(
    @Autowired(required = false)
    private val ociStorageProperties: OciStorageProperties?,
) {
    fun resolve(profileImageUrl: String?): String? {
        if (profileImageUrl == null) return null

        if (profileImageUrl.startsWith("http://") || profileImageUrl.startsWith("https://")) {
            return profileImageUrl
        }

        val baseUrl = ociStorageProperties?.baseUrl?.trimEnd('/') ?: return profileImageUrl
        return "$baseUrl/$profileImageUrl"
    }
}
