package com.wafflestudio.team8server.enrollmentperiod.repository

import com.wafflestudio.team8server.enrollmentperiod.model.EnrollmentPeriodConfig
import org.springframework.data.jpa.repository.JpaRepository

interface EnrollmentPeriodConfigRepository : JpaRepository<EnrollmentPeriodConfig, Int>
