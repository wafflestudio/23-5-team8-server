package com.wafflestudio.team8server.user.repository

import com.wafflestudio.team8server.admin.repository.DailyCountProjection
import com.wafflestudio.team8server.user.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface UserRepository : JpaRepository<User, Long> {
    @Query(
        value =
            """
            SELECT DATE(u.created_at) AS date,
                   COUNT(*)          AS count
            FROM users u
            WHERE u.created_at >= :fromAt
              AND u.created_at <  :toExclusive
            GROUP BY DATE(u.created_at)
            ORDER BY DATE(u.created_at) ASC
            """,
        nativeQuery = true,
    )
    fun countDailyNewUsers(
        @Param("fromAt") fromAt: LocalDateTime,
        @Param("toExclusive") toExclusive: LocalDateTime,
    ): List<DailyCountProjection>
}
