package com.wafflestudio.team8server.user.repository

import com.wafflestudio.team8server.user.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface UserRepository : JpaRepository<User, Long> {
    @Query(
        """
        SELECT CAST(u.createdAt AS LocalDate) AS date, COUNT(u) AS count
        FROM User u
        WHERE u.createdAt >= :startDateTime AND u.createdAt < :endDateTime
        GROUP BY CAST(u.createdAt AS LocalDate)
        ORDER BY date
        """,
    )
    fun countDailySignups(
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
    ): List<Array<Any>>
}
