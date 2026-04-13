package com.wafflestudio.team8server.syncwithsite.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "sync_with_site_runs")
class SyncWithSiteRun(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Enumerated(EnumType.STRING)
    val status: SyncWithSiteRunStatus,
    val startedAt: LocalDateTime,
    val finishedAt: LocalDateTime,
    @Column(columnDefinition = "TEXT")
    val dumpedData: String? = null,
    @Column(length = 1000)
    val message: String? = null,
)

enum class SyncWithSiteRunStatus {
    SUCCESS,
    FAILED,
}
