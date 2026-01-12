package com.wafflestudio.team8server.common.time

/**
 * 시간을 제공하는 인터페이스
 *
 * 테스트에서 시간을 제어할 수 있도록 추상화합니다.
 * 프로덕션에서는 실제 시스템 시간을 반환하고,
 * 테스트에서는 Mock 구현체를 사용하여 시간을 조작할 수 있습니다.
 */
interface TimeProvider {
    /**
     * 현재 시간을 밀리초 단위로 반환합니다.
     *
     * @return 현재 시간 (epoch 이후 밀리초)
     */
    fun currentTimeMillis(): Long
}
