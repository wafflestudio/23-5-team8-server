package com.wafflestudio.team8server.course.sync.repository

import com.wafflestudio.team8server.course.sync.model.CourseSyncSetting
import org.springframework.data.jpa.repository.JpaRepository

interface CourseSyncSettingRepository : JpaRepository<CourseSyncSetting, Long>
