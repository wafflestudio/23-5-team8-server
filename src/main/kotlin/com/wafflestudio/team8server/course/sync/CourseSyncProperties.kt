package com.wafflestudio.team8server.course.sync

import com.wafflestudio.team8server.course.model.Semester
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("course-sync")
class CourseSyncProperties(
    val auto: Auto = Auto(),
    val sugang: Sugang = Sugang(),
    val defaultTarget: DefaultTarget = DefaultTarget(),
) {
    data class Auto(
        val fixedDelayMillis: Long = 2 * 60 * 60 * 1000L, // 2 hours
    )

    data class Sugang(
        /**
         * 예: https://sugang.snu.ac.kr/...
         */
        val downloadUrlTemplate: String? = null,
    )

    data class DefaultTarget(
        val year: Int? = null,
        val semester: Semester? = null,
    )
}
