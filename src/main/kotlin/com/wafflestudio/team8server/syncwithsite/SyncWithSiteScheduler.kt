package com.wafflestudio.team8server.syncwithsite

import com.wafflestudio.team8server.syncwithsite.service.SyncWithSiteService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class SyncWithSiteScheduler(
    private val service: SyncWithSiteService,
) {
    // Crawl at every 24 hours
    @Scheduled(fixedDelayString = "\${sync-with-site.auto.fixedDelayMillis:86400000}")
    fun tick() {
        if (!service.isEnabled()) return
        service.runOnce()
    }
}
