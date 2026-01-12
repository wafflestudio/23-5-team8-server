package com.wafflestudio.team8server.common.time

/**
 * 테스트용 TimeProvider Mock 구현체
 *
 * 시간을 자유롭게 조작할 수 있어 테스트에서 시간 의존적인 로직을 검증할 수 있습니다.
 */
class MockTimeProvider(
    private var currentTime: Long = 0L,
) : TimeProvider {
    override fun currentTimeMillis(): Long = currentTime

    /**
     * 현재 시간을 설정합니다.
     *
     * @param timeMillis 설정할 시간 (epoch 이후 밀리초)
     */
    fun setTime(timeMillis: Long) {
        currentTime = timeMillis
    }

    /**
     * 현재 시간을 지정된 밀리초만큼 앞으로 진행시킵니다.
     *
     * @param millis 진행시킬 시간 (밀리초)
     */
    fun advance(millis: Long) {
        currentTime += millis
    }

    /**
     * 현재 시간을 초기화합니다.
     */
    fun reset() {
        currentTime = 0L
    }
}
