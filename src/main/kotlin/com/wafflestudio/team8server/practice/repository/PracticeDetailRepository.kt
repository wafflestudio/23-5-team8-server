package com.wafflestudio.team8server.practice.repository

import com.wafflestudio.team8server.admin.repository.DailyCountProjection
import com.wafflestudio.team8server.admin.repository.ReactionTimeByCourseAttributeProjection
import com.wafflestudio.team8server.admin.repository.ReactionTimeByCourseNumberProjection
import com.wafflestudio.team8server.practice.model.PracticeDetail
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface PracticeDetailRepository : JpaRepository<PracticeDetail, Long> {
    @Query("SELECT pd.reactionTime FROM PracticeDetail pd ORDER BY pd.reactionTime ASC")
    fun findAllReactionTimesOrderByAsc(): List<Int>

    fun countByPracticeLogId(practiceLogId: Long): Long

    fun findByPracticeLogIdOrderByReactionTimeAsc(practiceLogId: Long): List<PracticeDetail>

    fun findByPracticeLogIdAndCourseId(
        practiceLogId: Long,
        courseId: Long,
    ): PracticeDetail?

    fun countByPracticeLogIdAndIsSuccess(
        practiceLogId: Long,
        isSuccess: Boolean,
    ): Long

    fun findByPracticeLogIdOrderByIdAsc(practiceLogId: Long): List<PracticeDetail>

    @Query(
        value =
            """
            SELECT DATE(pl.practice_at) AS date,
                   COUNT(pd.id)        AS count
            FROM practice_details pd
            INNER JOIN practice_logs pl ON pl.id = pd.log_id
            WHERE pl.practice_at >= :fromAt
              AND pl.practice_at <  :toExclusive
            GROUP BY DATE(pl.practice_at)
            ORDER BY DATE(pl.practice_at) ASC
            """,
        nativeQuery = true,
    )
    fun countDailyPracticeDetails(
        @Param("fromAt") fromAt: LocalDateTime,
        @Param("toExclusive") toExclusive: LocalDateTime,
    ): List<DailyCountProjection>

    @Query(
        value = """
            SELECT FLOOR(pd.reaction_time / 10) AS bin_index,
                   COUNT(*)                     AS cnt
            FROM practice_details pd
            WHERE pd.reaction_time >= 0
              AND pd.reaction_time < 30000
            GROUP BY bin_index
            ORDER BY bin_index ASC
        """,
        nativeQuery = true,
    )
    fun countReactionTimeHistogramBins(): List<Array<Any>>

    @Query(
        value = """
            SELECT COUNT(*)
            FROM practice_details
            WHERE reaction_time >= 30000
        """,
        nativeQuery = true,
    )
    fun countReactionTimeOverflow(): Long

    /**
     * Groups reaction time statistics by course classification (e.g., "전공필수", "전공선택", "교양").
     *
     * Each row in the result represents one classification group and contains:
     * - attribute: the classification name (e.g., "전공필수")
     * - count: total number of practice attempts for courses in this classification
     * - avgReactionTime: average reaction time in ms across all attempts in this group
     * - minReactionTime: fastest reaction time recorded in this group (ms)
     * - maxReactionTime: slowest reaction time recorded in this group (ms)
     * - successCount: number of attempts where the user successfully registered
     *
     * Only includes attempts that are linked to an existing course (course_id IS NOT NULL).
     * Results are ordered by avgReactionTime ascending (fastest group first).
     */
    @Query(
        value = """
            SELECT c.classification       AS attribute,
                   COUNT(pd.id)           AS count,
                   AVG(pd.reaction_time)  AS avgReactionTime,
                   MIN(pd.reaction_time)  AS minReactionTime,
                   MAX(pd.reaction_time)  AS maxReactionTime,
                   SUM(pd.is_success)     AS successCount
            FROM practice_details pd
            INNER JOIN courses c ON pd.course_id = c.id
            GROUP BY c.classification
            ORDER BY avgReactionTime ASC
        """,
        nativeQuery = true,
    )
    fun groupReactionTimeByClassification(): List<ReactionTimeByCourseAttributeProjection>

    /**
     * Groups reaction time statistics by college (단과대학, e.g., "공과대학", "인문대학").
     *
     * Each row in the result represents one college and contains:
     * - attribute: the college name (e.g., "공과대학")
     * - count: total number of practice attempts for courses offered by this college
     * - avgReactionTime: average reaction time in ms across all attempts in this group
     * - minReactionTime: fastest reaction time recorded in this group (ms)
     * - maxReactionTime: slowest reaction time recorded in this group (ms)
     * - successCount: number of attempts where the user successfully registered
     *
     * Only includes attempts that are linked to an existing course (course_id IS NOT NULL).
     * Results are ordered by avgReactionTime ascending (fastest group first).
     */
    @Query(
        value = """
            SELECT c.college              AS attribute,
                   COUNT(pd.id)           AS count,
                   AVG(pd.reaction_time)  AS avgReactionTime,
                   MIN(pd.reaction_time)  AS minReactionTime,
                   MAX(pd.reaction_time)  AS maxReactionTime,
                   SUM(pd.is_success)     AS successCount
            FROM practice_details pd
            INNER JOIN courses c ON pd.course_id = c.id
            GROUP BY c.college
            ORDER BY avgReactionTime ASC
        """,
        nativeQuery = true,
    )
    fun groupReactionTimeByCollege(): List<ReactionTimeByCourseAttributeProjection>

    /**
     * Groups reaction time statistics by department (학과, e.g., "컴퓨터공학부", "경제학부").
     *
     * Each row in the result represents one department and contains:
     * - attribute: the department name (e.g., "컴퓨터공학부")
     * - count: total number of practice attempts for courses offered by this department
     * - avgReactionTime: average reaction time in ms across all attempts in this group
     * - minReactionTime: fastest reaction time recorded in this group (ms)
     * - maxReactionTime: slowest reaction time recorded in this group (ms)
     * - successCount: number of attempts where the user successfully registered
     *
     * Only includes attempts that are linked to an existing course (course_id IS NOT NULL).
     * Results are ordered by avgReactionTime ascending (fastest group first).
     */
    @Query(
        value = """
            SELECT c.department           AS attribute,
                   COUNT(pd.id)           AS count,
                   AVG(pd.reaction_time)  AS avgReactionTime,
                   MIN(pd.reaction_time)  AS minReactionTime,
                   MAX(pd.reaction_time)  AS maxReactionTime,
                   SUM(pd.is_success)     AS successCount
            FROM practice_details pd
            INNER JOIN courses c ON pd.course_id = c.id
            GROUP BY c.department
            ORDER BY avgReactionTime ASC
        """,
        nativeQuery = true,
    )
    fun groupReactionTimeByDepartment(): List<ReactionTimeByCourseAttributeProjection>

    /**
     * Groups reaction time statistics by academic course type (과정, e.g., "학사", "석사", "박사").
     *
     * Each row in the result represents one academic course type and contains:
     * - attribute: the academic course type (e.g., "학사")
     * - count: total number of practice attempts for courses in this academic course type
     * - avgReactionTime: average reaction time in ms across all attempts in this group
     * - minReactionTime: fastest reaction time recorded in this group (ms)
     * - maxReactionTime: slowest reaction time recorded in this group (ms)
     * - successCount: number of attempts where the user successfully registered
     *
     * Only includes attempts that are linked to an existing course (course_id IS NOT NULL).
     * Results are ordered by avgReactionTime ascending (fastest group first).
     */
    @Query(
        value = """
            SELECT c.academic_course      AS attribute,
                   COUNT(pd.id)           AS count,
                   AVG(pd.reaction_time)  AS avgReactionTime,
                   MIN(pd.reaction_time)  AS minReactionTime,
                   MAX(pd.reaction_time)  AS maxReactionTime,
                   SUM(pd.is_success)     AS successCount
            FROM practice_details pd
            INNER JOIN courses c ON pd.course_id = c.id
            GROUP BY c.academic_course
            ORDER BY avgReactionTime ASC
        """,
        nativeQuery = true,
    )
    fun groupReactionTimeByAcademicCourse(): List<ReactionTimeByCourseAttributeProjection>

    /**
     * Groups reaction time statistics by academic year (학년, e.g., "1학년", "2학년", "전학년").
     *
     * Each row in the result represents one academic year group and contains:
     * - attribute: the academic year value (e.g., "2학년")
     * - count: total number of practice attempts for courses targeted at this academic year
     * - avgReactionTime: average reaction time in ms across all attempts in this group
     * - minReactionTime: fastest reaction time recorded in this group (ms)
     * - maxReactionTime: slowest reaction time recorded in this group (ms)
     * - successCount: number of attempts where the user successfully registered
     *
     * Only includes attempts that are linked to an existing course (course_id IS NOT NULL).
     * Results are ordered by avgReactionTime ascending (fastest group first).
     */
    @Query(
        value = """
            SELECT c.academic_year        AS attribute,
                   COUNT(pd.id)           AS count,
                   AVG(pd.reaction_time)  AS avgReactionTime,
                   MIN(pd.reaction_time)  AS minReactionTime,
                   MAX(pd.reaction_time)  AS maxReactionTime,
                   SUM(pd.is_success)     AS successCount
            FROM practice_details pd
            INNER JOIN courses c ON pd.course_id = c.id
            GROUP BY c.academic_year
            ORDER BY avgReactionTime ASC
        """,
        nativeQuery = true,
    )
    fun groupReactionTimeByAcademicYear(): List<ReactionTimeByCourseAttributeProjection>

    /**
     * Groups reaction time statistics by course credit (학점, e.g., 1, 2, 3).
     *
     * credit is an Int in the Course entity, so it is cast to CHAR to fit the String-typed
     * 'attribute' field in ReactionTimeByCourseAttributeProjection.
     *
     * Each row in the result represents one credit value and contains:
     * - attribute: the credit as a string (e.g., "3")
     * - count: total number of practice attempts for courses with this credit value
     * - avgReactionTime: average reaction time in ms across all attempts in this group
     * - minReactionTime: fastest reaction time recorded in this group (ms)
     * - maxReactionTime: slowest reaction time recorded in this group (ms)
     * - successCount: number of attempts where the user successfully registered
     *
     * Only includes attempts that are linked to an existing course (course_id IS NOT NULL).
     * Results are ordered by avgReactionTime ascending (fastest group first).
     */
    @Query(
        value = """
            SELECT CAST(c.credit AS CHAR)  AS attribute,
                   COUNT(pd.id)            AS count,
                   AVG(pd.reaction_time)   AS avgReactionTime,
                   MIN(pd.reaction_time)   AS minReactionTime,
                   MAX(pd.reaction_time)   AS maxReactionTime,
                   SUM(pd.is_success)      AS successCount
            FROM practice_details pd
            INNER JOIN courses c ON pd.course_id = c.id
            GROUP BY c.credit
            ORDER BY avgReactionTime ASC
        """,
        nativeQuery = true,
    )
    fun groupReactionTimeByCredit(): List<ReactionTimeByCourseAttributeProjection>

    /**
     * Groups reaction time statistics by course number (교과목 번호, e.g., "M1234", "E5678").
     *
     * NOTE: This query treats all sections (분반) with the same courseNumber as the same course.
     * This means section-specific restrictions (e.g., "수리과학부 분반만 가능", "주전공 불허") are NOT
     * reflected — all sections are merged into one group. See GitHub issue for future improvement.
     *
     * Each row in the result represents one course (across all its sections) and contains:
     * - attribute: the course number (e.g., "M1234")
     * - count: total number of practice attempts across all sections of this course
     * - avgReactionTime: average reaction time in ms across all attempts in this group
     * - minReactionTime: fastest reaction time recorded in this group (ms)
     * - maxReactionTime: slowest reaction time recorded in this group (ms)
     * - successCount: number of attempts where the user successfully registered
     *
     * Only includes attempts that are linked to an existing course (course_id IS NOT NULL).
     * Results are ordered by avgReactionTime ascending (fastest group first).
     */
    @Query(
        value = """
            SELECT c.course_number        AS attribute,
                   MIN(c.course_name)     AS courseName,
                   COUNT(pd.id)           AS count,
                   AVG(pd.reaction_time)  AS avgReactionTime,
                   MIN(pd.reaction_time)  AS minReactionTime,
                   MAX(pd.reaction_time)  AS maxReactionTime,
                   SUM(pd.is_success)     AS successCount
            FROM practice_details pd
            INNER JOIN courses c ON pd.course_id = c.id
            GROUP BY c.course_number
            ORDER BY avgReactionTime ASC
        """,
        nativeQuery = true,
    )
    fun groupReactionTimeByCourseNumber(): List<ReactionTimeByCourseNumberProjection>
}
