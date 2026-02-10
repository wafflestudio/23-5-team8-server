package com.wafflestudio.team8server.admin.repository

import java.time.LocalDate

interface DailyCountProjection {
    fun getDate(): LocalDate

    fun getCount(): Long
}
