package com.wafflestudio.team8server.practice.service

import com.wafflestudio.team8server.practice.config.PracticeSessionConfig
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * Redis를 사용한 수강신청 연습 세션 관리 서비스
 *
 * 세션 정보:
 * - Key: "practice:session:{userId}" → practiceLogId (연습 세션 ID)
 * - TTL: config에서 설정된 세션 유지 시간
 */
@Service
class PracticeSessionService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val config: PracticeSessionConfig,
) {
    companion object {
        private const val SESSION_KEY_PREFIX = "practice:session:"
        private const val SESSION_LOCK_KEY_SUFFIX = ":lock"
        private const val SESSION_START_TIME_SUFFIX = ":startTime"
    }

    /**
     * 사용자의 활성 세션을 생성합니다.
     *
     * @param userId 사용자 ID
     * @param practiceLogId 연습 세션 ID
     */
    fun createSession(
        userId: Long,
        practiceLogId: Long,
    ) {
        val sessionKey = SESSION_KEY_PREFIX + userId

        // practiceLogId 저장 (TTL 자동 설정)
        redisTemplate.opsForValue().set(
            sessionKey,
            practiceLogId.toString(),
            config.timeLimitSeconds,
            TimeUnit.SECONDS,
        )
    }

    /**
     * 사용자의 활성 세션 ID를 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 활성 세션 ID (없으면 null)
     */
    fun getActiveSession(userId: Long): Long? {
        val key = SESSION_KEY_PREFIX + userId
        val value = redisTemplate.opsForValue().get(key)
        return value?.toLongOrNull()
    }

    /**
     * 사용자의 활성 세션이 있는지 확인합니다.
     *
     * @param userId 사용자 ID
     * @return 활성 세션 존재 여부
     */
    fun hasActiveSession(userId: Long): Boolean {
        val key = SESSION_KEY_PREFIX + userId
        return redisTemplate.hasKey(key) ?: false
    }

    /**
     * 사용자의 활성 세션을 종료합니다.
     *
     * @param userId 사용자 ID
     */
    fun endSession(userId: Long) {
        val sessionKey = SESSION_KEY_PREFIX + userId
        redisTemplate.delete(sessionKey)
    }

    /**
     * 세션의 남은 TTL을 초 단위로 반환합니다.
     *
     * @param userId 사용자 ID
     * @return 남은 TTL (초), 세션이 없으면 null
     */
    fun getSessionTTL(userId: Long): Long? {
        val key = SESSION_KEY_PREFIX + userId
        val ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS)
        return if (ttl > 0) ttl else null
    }

    /**
     * 분산 락을 획득합니다.
     *
     * @param userId 사용자 ID
     * @return 락 획득 성공 여부
     */
    fun acquireLock(userId: Long): Boolean {
        val lockKey = SESSION_KEY_PREFIX + userId + SESSION_LOCK_KEY_SUFFIX
        val result =
            redisTemplate.opsForValue().setIfAbsent(
                lockKey,
                "locked",
                config.lockTtlSeconds,
                TimeUnit.SECONDS,
            )
        return result ?: false
    }

    /**
     * 분산 락을 해제합니다.
     *
     * @param userId 사용자 ID
     */
    fun releaseLock(userId: Long) {
        val lockKey = SESSION_KEY_PREFIX + userId + SESSION_LOCK_KEY_SUFFIX
        redisTemplate.delete(lockKey)
    }

    /**
     * 세션 시작 시간을 저장합니다.
     *
     * @param userId 사용자 ID
     * @param startTimeMs 시작 시간 (epoch 이후 밀리초)
     */
    fun saveStartTime(
        userId: Long,
        startTimeMs: Long,
    ) {
        val startTimeKey = SESSION_KEY_PREFIX + userId + SESSION_START_TIME_SUFFIX
        redisTemplate.opsForValue().set(
            startTimeKey,
            startTimeMs.toString(),
            config.timeLimitSeconds,
            TimeUnit.SECONDS,
        )
    }

    /**
     * 세션 시작 시간을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 시작 시간 (epoch 이후 밀리초), 없으면 null
     */
    fun getStartTime(userId: Long): Long? {
        val startTimeKey = SESSION_KEY_PREFIX + userId + SESSION_START_TIME_SUFFIX
        val value = redisTemplate.opsForValue().get(startTimeKey)
        return value?.toLongOrNull()
    }
}
