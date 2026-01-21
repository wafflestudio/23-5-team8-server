package com.wafflestudio.team8server.practice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.listener.PatternTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer

@Configuration
class PracticeRedisConfig {
    /**
     * Redis Keyspace Notifications를 수신하기 위한 리스너 컨테이너 설정.
     * "__keyevent@*__:expired" 패턴으로 모든 DB의 키 만료 이벤트를 수신합니다.
     *
     * 주의: Redis 서버에서 notify-keyspace-events 설정이 "Ex" 이상이어야 합니다.
     * - E: Keyevent events
     * - x: Expired events
     */
    @Bean
    fun redisMessageListenerContainer(
        connectionFactory: RedisConnectionFactory,
        sessionExpirationListener: PracticeSessionExpirationListener,
    ): RedisMessageListenerContainer {
        val container = RedisMessageListenerContainer()
        container.setConnectionFactory(connectionFactory)

        // 키 만료 이벤트 구독 (모든 DB의 expired 이벤트)
        container.addMessageListener(
            sessionExpirationListener,
            PatternTopic("__keyevent@*__:expired"),
        )

        return container
    }
}
