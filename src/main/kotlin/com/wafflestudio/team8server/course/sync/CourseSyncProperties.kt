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
        val excelUrl: String = "https://sugang.snu.ac.kr/sugang/cc/cc100InterfaceExcel.action",
        val refererUrl: String = "https://sugang.snu.ac.kr/sugang/cc/cc100InterfaceSrch.action",
        val pageSize: Int = 9999,
        val language: String = "ko",
        val connectTimeoutMillis: Long = 5_000,
        val readTimeoutMillis: Long = 30_000,
    )

    data class DefaultTarget(
        val year: Int? = null,
        val semester: Semester? = null,
    )
}
