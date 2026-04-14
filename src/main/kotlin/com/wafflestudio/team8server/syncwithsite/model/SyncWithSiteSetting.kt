package com.wafflestudio.team8server.syncwithsite.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "sync_with_site_settings")
class SyncWithSiteSetting(
    @Id
    val id: Long = 1L,
    var enabled: Boolean = false,
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
