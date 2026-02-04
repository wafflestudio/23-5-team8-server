package com.wafflestudio.team8server.practice.model

import com.wafflestudio.team8server.course.model.Course
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "practice_details")
class PracticeDetail(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_id", nullable = false)
    val practiceLog: PracticeLog,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = true)
    val course: Course?,
    @Column(name = "course_title", nullable = true, length = 255)
    val courseTitle: String?,
    @Column(name = "lecture_number", nullable = true, length = 10)
    val lectureNumber: String?,
    @Column(name = "is_success", nullable = false)
    val isSuccess: Boolean,
    @Column(name = "reaction_time", nullable = false)
    val reactionTime: Int,
    @Column(name = "user_rank", nullable = false)
    val rank: Int,
    @Column(name = "percentile", nullable = false)
    val percentile: Double,
    @Column(name = "capacity", nullable = false)
    val capacity: Int,
    @Column(name = "total_competitors", nullable = false)
    val totalCompetitors: Int,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
