package com.wafflestudio.team8server.user.enum

enum class SocialProvider {
    KAKAO,
    GOOGLE,
    ;

    fun dbValue(): String = name.lowercase()
}
