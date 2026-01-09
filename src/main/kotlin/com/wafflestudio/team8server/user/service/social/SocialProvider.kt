package com.wafflestudio.team8server.user.service.social

enum class SocialProvider {
    KAKAO, GOOGLE;

    fun dbValue(): String = name.lowercase()
}