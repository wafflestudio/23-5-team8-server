package com.wafflestudio.team8server.enrollmentperiod.model

import com.wafflestudio.team8server.config.EnrollmentPeriodType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "enrollment_period_config")
class EnrollmentPeriodConfig(
    @Id
    val id: Int = 1,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var type: EnrollmentPeriodType,
)
