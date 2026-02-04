package com.wafflestudio.team8server.course.sync.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "course_sync_settings")
class CourseSyncSetting(
    @Id
    val id: Long = 1L,
    @Column(nullable = false)
    val enabled: Boolean = false,
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
