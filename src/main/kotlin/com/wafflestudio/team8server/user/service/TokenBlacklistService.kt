package com.wafflestudio.team8server.user.service

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class TokenBlacklistService(
    private val redisTemplate: RedisTemplate<String, String>,
) {
    companion object {
        private const val BLACKLIST_KEY_PREFIX = "token:blacklist:"
    }

    fun addToBlacklist(
        token: String,
        expirationInMs: Long,
    ) {
        val key = BLACKLIST_KEY_PREFIX + token
        // "blacklisted"라는 값으로 저장하고, 토큰의 남은 유효시간만큼 TTL 설정
        redisTemplate.opsForValue().set(key, "blacklisted", expirationInMs, TimeUnit.MILLISECONDS)
    }

    fun isBlacklisted(token: String): Boolean {
        val key = BLACKLIST_KEY_PREFIX + token
        // Redis에 해당 키가 존재하면 블랙리스트에 있는 것
        return redisTemplate.hasKey(key) ?: false
    }
}
