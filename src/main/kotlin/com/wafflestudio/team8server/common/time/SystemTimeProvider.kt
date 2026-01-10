package com.wafflestudio.team8server.common.time

import org.springframework.stereotype.Component

/**
 * 실제 시스템 시간을 제공하는 구현체
 *
 * 프로덕션 환경에서 사용됩니다.
 */
@Component
class SystemTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}
