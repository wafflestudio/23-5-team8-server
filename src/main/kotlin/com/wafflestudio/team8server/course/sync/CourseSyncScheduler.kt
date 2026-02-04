package com.wafflestudio.team8server.course.sync

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class CourseSyncScheduler(
    private val service: CourseSyncService,
) {
    private val log = LoggerFactory.getLogger(CourseSyncScheduler::class.java)

    @Scheduled(fixedDelayString = "\${course-sync.auto.fixedDelayMillis:7200000}")
    fun tick() {
        if (!service.isEnabled()) return

        val target = service.defaultTarget()
        if (target == null) {
            log.warn("Auto course sync is enabled but courseSync.defaultTarget is not configured. Skipping.")
            return
        }

        val (year, semester) = target
        service.runOnce(year, semester)
    }
}
