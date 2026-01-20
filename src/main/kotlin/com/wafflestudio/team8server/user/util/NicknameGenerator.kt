package com.wafflestudio.team8server.user.util

import kotlin.random.Random

object NicknameGenerator {
    private const val CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

    fun generateRandomNickname(): String {
        val length = Random.nextInt(2, 7) // 2~6자
        return (1..length)
            .map { CHARS[Random.nextInt(CHARS.length)] }
            .joinToString("")
    }
}
