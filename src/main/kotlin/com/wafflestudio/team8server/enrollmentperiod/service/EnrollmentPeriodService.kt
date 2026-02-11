package com.wafflestudio.team8server.enrollmentperiod.service

import com.wafflestudio.team8server.config.EnrollmentPeriodProperties
import com.wafflestudio.team8server.config.EnrollmentPeriodType
import com.wafflestudio.team8server.enrollmentperiod.model.EnrollmentPeriodConfig
import com.wafflestudio.team8server.enrollmentperiod.repository.EnrollmentPeriodConfigRepository
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EnrollmentPeriodService(
    private val enrollmentPeriodProperties: EnrollmentPeriodProperties,
    private val enrollmentPeriodConfigRepository: EnrollmentPeriodConfigRepository,
) {
    /**
     * 애플리케이션 부팅 시 DB에 저장된 period type을 사용합니다.
     *
     * DB row가 없다면 application.yaml/env의 enrollment-period.type 값을
     * 그대로 DB에 저장하고 사용합니다.
     */
    @PostConstruct
    @Transactional
    fun initializeFromDb() {
        val config =
            enrollmentPeriodConfigRepository.findById(DEFAULT_ROW_ID).orElseGet {
                enrollmentPeriodConfigRepository.save(
                    EnrollmentPeriodConfig(
                        id = DEFAULT_ROW_ID,
                        type = enrollmentPeriodProperties.type,
                    ),
                )
            }

        // 런타임에는 properties.type을 캐시로 사용합니다.
        enrollmentPeriodProperties.type = config.type
    }

    @Transactional(readOnly = true)
    fun getCurrentType(): EnrollmentPeriodType = enrollmentPeriodProperties.type

    @Transactional
    fun updateType(newType: EnrollmentPeriodType): EnrollmentPeriodType {
        val config =
            enrollmentPeriodConfigRepository.findById(DEFAULT_ROW_ID).orElseGet {
                EnrollmentPeriodConfig(id = DEFAULT_ROW_ID, type = newType)
            }

        config.type = newType
        enrollmentPeriodConfigRepository.save(config)

        enrollmentPeriodProperties.type = newType
        return newType
    }

    companion object {
        private const val DEFAULT_ROW_ID: Int = 1
    }
}
