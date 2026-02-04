package com.wafflestudio.team8server.course.sync.repository

import com.wafflestudio.team8server.course.sync.model.CourseSyncRun
import org.springframework.data.jpa.repository.JpaRepository

interface CourseSyncRunRepository : JpaRepository<CourseSyncRun, Long> {
    fun findTopByOrderByStartedAtDesc(): CourseSyncRun?
}
