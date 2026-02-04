package com.wafflestudio.team8server.course.sync.model

import com.wafflestudio.team8server.course.model.Semester
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
@Table(name = "course_sync_runs")
class CourseSyncRun(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val status: CourseSyncRunStatus,
    @Column(name = "started_at", nullable = false)
    val startedAt: LocalDateTime,
    @Column(name = "finished_at")
    val finishedAt: LocalDateTime? = null,
    @Column(nullable = false)
    val year: Int,
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val semester: Semester,
    @Column(name = "rows_upserted")
    val rowsUpserted: Int? = null,
    @Column(length = 500)
    val message: String? = null,
)

enum class CourseSyncRunStatus {
    SUCCESS,
    FAILED,
}
